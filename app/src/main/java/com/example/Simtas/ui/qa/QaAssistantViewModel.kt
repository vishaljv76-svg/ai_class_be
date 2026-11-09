package com.example.Simtas.ui.qa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.ml.OfflineSpeechRecognizer
import com.example.Simtas.data.ml.QuestionAnsweringEngine
import com.example.Simtas.data.ml.WhisperModelManager
import com.example.Simtas.data.repository.ChatRepository
import com.example.Simtas.data.repository.ChatResult
import com.example.Simtas.network.ChatConversationItem
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageSource {
    USER,
    ONLINE_GEMINI,
    OFFLINE_ENGINE
}

data class QaMessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSource,
    val text: String,
    val explanation: String = "",
    val provider: String? = null,
    val isAnswered: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class QaAssistantUiState(
    val messages: List<QaMessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val conversationId: Int? = null,
    val conversationTitle: String? = null,
    val errorMessage: String? = null,
    val isOfflineModeActive: Boolean = false,
    val isListening: Boolean = false,
    val isTranscribingVoice: Boolean = false,
    val voiceProgress: Int = 0,
    val selectedLanguageCode: String = "en",
    val conversations: List<ChatConversationItem> = emptyList(),
    val isLoadingConversations: Boolean = false
)

class QaAssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QaAssistantUiState())
    val uiState: StateFlow<QaAssistantUiState> = _uiState.asStateFlow()

    // --- Voice Recording & Offline Speech Recognition State ---
    private var voiceRecorder: MediaRecorder? = null
    private var currentVoiceFile: File? = null
    private val voiceLock = Any()
    private val downloadLock = Any()

    @Volatile
    private var isDownloadingModel = false

    /**
     * Toggles or selects the recognition language ("en" for English, "ta" for Tamil).
     */
    fun setLanguage(languageCode: String) {
        _uiState.update { it.copy(selectedLanguageCode = languageCode) }
    }

    /**
     * Starts microphone recording for ChatGPT-style voice questioning.
     */
    fun startVoiceRecording(context: Context) {
        synchronized(voiceLock) {
            if (_uiState.value.isListening || voiceRecorder != null) return

            // Permission check
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                _uiState.update {
                    it.copy(errorMessage = "Microphone permission is required for voice questions.")
                }
                return
            }

            try {
                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) recordingsDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(recordingsDir, "voice_qa_${timestamp}.m4a")
                currentVoiceFile = outputFile

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }

                voiceRecorder = recorder
                _uiState.update {
                    it.copy(
                        isListening = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                releaseVoiceRecorder()
                _uiState.update {
                    it.copy(
                        isListening = false,
                        errorMessage = "Microphone error: ${e.localizedMessage ?: "Unable to access microphone"}"
                    )
                }
            }
        }
    }

    /**
     * Stops audio recording and triggers local offline transcription with Sherpa-ONNX.
     */
    fun stopVoiceRecording(
        context: Context,
        onTranscriptReady: (String) -> Unit
    ) {
        synchronized(voiceLock) {
            val recorder = voiceRecorder
            if (recorder != null) {
                try {
                    recorder.stop()
                } catch (_: Exception) {}
                try {
                    recorder.reset()
                    recorder.release()
                } catch (_: Exception) {}
            }
            voiceRecorder = null
            _uiState.update { it.copy(isListening = false) }

            val file = currentVoiceFile
            if (file != null && file.exists() && file.length() > 0L) {
                transcribeVoiceFile(context, file, onTranscriptReady)
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "No speech detected. Please tap mic and speak again."
                    )
                }
            }
        }
    }

    /**
     * Cancels voice recording without transcribing.
     */
    fun cancelVoiceRecording() {
        synchronized(voiceLock) {
            releaseVoiceRecorder()
            _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun releaseVoiceRecorder() {
        try {
            voiceRecorder?.stop()
        } catch (_: Exception) {}
        try {
            voiceRecorder?.reset()
            voiceRecorder?.release()
        } catch (_: Exception) {}
        voiceRecorder = null
    }

    /**
     * Public helper to transcribe an attached audio lecture/voice file.
     */
    fun transcribeAudioFile(
        context: Context,
        audioFile: File,
        onTranscriptReady: (String) -> Unit
    ) {
        transcribeVoiceFile(context, audioFile, onTranscriptReady)
    }

    /**
     * Ensures offline Whisper model files (~99 MB) are available before transcription.
     */
    private suspend fun ensureModelInstalled(context: Context): Boolean {
        if (WhisperModelManager.isModelInstalled(context)) {
            return true
        }

        synchronized(downloadLock) {
            if (isDownloadingModel) {
                _uiState.update {
                    it.copy(
                        isTranscribingVoice = true,
                        errorMessage = null
                    )
                }
                return false
            }
            isDownloadingModel = true
        }

        _uiState.update {
            it.copy(
                isTranscribingVoice = true,
                voiceProgress = 0,
                errorMessage = null
            )
        }

        val downloadResult = try {
            WhisperModelManager.downloadModel(context) { progress ->
                _uiState.update {
                    it.copy(
                        voiceProgress = progress.percentage
                    )
                }
            }
        } finally {
            synchronized(downloadLock) {
                isDownloadingModel = false
            }
        }

        return if (downloadResult.isSuccess && WhisperModelManager.isModelInstalled(context)) {
            _uiState.update {
                it.copy(
                    voiceProgress = 10,
                    errorMessage = null
                )
            }
            true
        } else {
            val err = downloadResult.exceptionOrNull()?.localizedMessage
                ?: "Failed to download offline speech model. An internet connection is required for first-time setup."
            _uiState.update {
                it.copy(
                    isTranscribingVoice = false,
                    errorMessage = err
                )
            }
            false
        }
    }

    /**
     * Transcribes recorded or attached audio file on-device using Sherpa-ONNX Whisper.
     */
    private fun transcribeVoiceFile(
        context: Context,
        file: File,
        onTranscriptReady: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!ensureModelInstalled(context)) {
                return@launch
            }

            _uiState.update {
                it.copy(
                    isTranscribingVoice = true,
                    voiceProgress = 25,
                    errorMessage = null
                )
            }

            val lang = _uiState.value.selectedLanguageCode
            val result = OfflineSpeechRecognizer.transcribeAudioFile(
                context = context,
                audioFile = file,
                languageCode = lang,
                onProgress = { p ->
                    _uiState.update { it.copy(voiceProgress = p) }
                }
            )

            withContext(Dispatchers.Main) {
                if (result.isSuccess && result.fullTranscript.isNotBlank()) {
                    val transcript = result.fullTranscript.trim()
                    _uiState.update {
                        it.copy(
                            isTranscribingVoice = false,
                            voiceProgress = 100,
                            errorMessage = null
                        )
                    }
                    onTranscriptReady(transcript)
                } else {
                    val err = result.errorMessage
                        ?: "Speech could not be recognized. Please tap the mic and try speaking again."
                    _uiState.update {
                        it.copy(
                            isTranscribingVoice = false,
                            voiceProgress = 0,
                            errorMessage = err
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseVoiceRecorder()
    }

    // --- Text Q&A and Chat API Methods ---

    fun askQuestion(
        question: String,
        subject: String? = null,
        lectureTitle: String? = null,
        transcriptText: String = "",
        detailedNotes: DetailedNotesUiState? = null,
        summaryText: String = "",
        keyTakeaways: List<String> = emptyList(),
        forceOffline: Boolean = false
    ) {
        val q = question.trim()
        if (q.isBlank()) return
        if (_uiState.value.isLoading) return // Prevent duplicate requests while loading

        // Add user message to UI immediately
        val userMsg = QaMessageItem(
            sender = MessageSource.USER,
            text = q,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                messages = listOf(userMsg) + it.messages,
                isLoading = true,
                errorMessage = null
            )
        }

        if (forceOffline) {
            executeOfflineAnswer(q, transcriptText, detailedNotes, summaryText, keyTakeaways)
            return
        }

        viewModelScope.launch {
            val currentConvId = _uiState.value.conversationId
            val result = ChatRepository.sendMessage(
                message = q,
                conversationId = currentConvId,
                subject = subject,
                lectureTitle = lectureTitle
            )

            when (result) {
                is ChatResult.Success -> {
                    val data = result.data
                    val assistantText = data.assistantMessage?.content ?: data.message
                    val provider = data.assistantMessage?.provider ?: "gemini"

                    val assistantMsg = QaMessageItem(
                        sender = MessageSource.ONLINE_GEMINI,
                        text = assistantText,
                        provider = provider,
                        isAnswered = true,
                        timestamp = System.currentTimeMillis()
                    )

                    _uiState.update {
                        it.copy(
                            messages = listOf(assistantMsg) + it.messages,
                            conversationId = data.conversationId,
                            conversationTitle = data.conversationTitle,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    // Refresh conversation list after new message
                    fetchConversations()
                }
                is ChatResult.Error -> {
                    // Graceful offline fallback with clear labelling
                    val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
                        question = q,
                        transcriptText = transcriptText,
                        detailedNotes = detailedNotes,
                        summaryText = summaryText,
                        keyTakeaways = keyTakeaways
                    )

                    val offlineMsg = QaMessageItem(
                        sender = MessageSource.OFFLINE_ENGINE,
                        text = offlineAnswer.answer,
                        explanation = if (offlineAnswer.explanation.isNotBlank()) {
                            "${offlineAnswer.explanation} (Note: Offline Fallback due to ${result.message})"
                        } else {
                            "Offline Fallback (Live Gemini unavailable: ${result.message})"
                        },
                        provider = "Offline Curriculum Knowledge",
                        isAnswered = offlineAnswer.isAnswered,
                        timestamp = System.currentTimeMillis()
                    )

                    _uiState.update {
                        it.copy(
                            messages = listOf(offlineMsg) + it.messages,
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun executeOfflineAnswer(
        question: String,
        transcriptText: String,
        detailedNotes: DetailedNotesUiState?,
        summaryText: String,
        keyTakeaways: List<String>
    ) {
        val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
            question = question,
            transcriptText = transcriptText,
            detailedNotes = detailedNotes,
            summaryText = summaryText,
            keyTakeaways = keyTakeaways
        )

        val offlineMsg = QaMessageItem(
            sender = MessageSource.OFFLINE_ENGINE,
            text = offlineAnswer.answer,
            explanation = offlineAnswer.explanation,
            provider = "Offline Curriculum Knowledge",
            isAnswered = offlineAnswer.isAnswered,
            timestamp = System.currentTimeMillis()
        )

        _uiState.update {
            it.copy(
                messages = listOf(offlineMsg) + it.messages,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun loadConversationHistory(conversationId: Int) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = ChatRepository.getHistory(conversationId)
            when (result) {
                is ChatResult.Success -> {
                    val historyItems = result.data.messages.mapIndexed { index, msg ->
                        val isUser = msg.role.equals("user", ignoreCase = true)
                        QaMessageItem(
                            sender = if (isUser) MessageSource.USER else MessageSource.ONLINE_GEMINI,
                            text = msg.message_content,
                            provider = msg.provider ?: if (isUser) null else "gemini",
                            timestamp = System.currentTimeMillis() - (result.data.messages.size - index) * 1000L
                        )
                    }.reversed()

                    _uiState.update {
                        it.copy(
                            messages = historyItems,
                            conversationId = result.data.conversationId,
                            conversationTitle = result.data.title,
                            isLoading = false
                        )
                    }
                }
                is ChatResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun fetchConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversations = true) }
            val result = ChatRepository.listConversations()
            when (result) {
                is ChatResult.Success -> {
                    _uiState.update {
                        it.copy(
                            conversations = result.data,
                            isLoadingConversations = false
                        )
                    }
                }
                is ChatResult.Error -> {
                    _uiState.update {
                        it.copy(isLoadingConversations = false)
                    }
                }
            }
        }
    }

    fun startNewChat() {
        _uiState.update {
            it.copy(
                conversationId = null,
                conversationTitle = null,
                messages = emptyList(),
                errorMessage = null,
                isLoading = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startNewConversation() {
        startNewChat()
    }
}
