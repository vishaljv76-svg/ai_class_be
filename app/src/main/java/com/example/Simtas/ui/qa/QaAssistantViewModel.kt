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
import com.example.Simtas.data.repository.ChatStreamEvent
import com.example.Simtas.network.ChatConversationItem
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
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
    val timestamp: Long = System.currentTimeMillis(),
    val imageBitmap: Bitmap? = null,
    val imageUri: String? = null,
    val isStreaming: Boolean = false,
    val attachmentId: Int? = null,
    val isImageLoading: Boolean = false,
    val imageLoadFailed: Boolean = false
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
    val isLoadingConversations: Boolean = false,
    val selectedImageUri: String? = null,
    val selectedImageBitmap: Bitmap? = null,
    val selectedImageBase64: String? = null,
    val selectedImageMime: String? = null
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

    @Volatile
    internal var geminiRateLimitedUntil: Long = 0L

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

    // --- Image Understanding & Multimodal Handlers ---

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = decodeSampledBitmapFromUri(context, uri, 1024, 1024)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val bytes = baos.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                selectedImageUri = uri.toString(),
                                selectedImageBitmap = bitmap,
                                selectedImageBase64 = base64,
                                selectedImageMime = "image/jpeg",
                                errorMessage = null
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(errorMessage = "Could not decode selected image.") }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "Failed to process image: ${e.localizedMessage}") }
                }
            }
        }
    }

    fun onPhotoCaptured(context: Context, photoFile: File, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = decodeSampledBitmapFromFile(photoFile, 1024, 1024)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val bytes = baos.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                selectedImageUri = uri.toString(),
                                selectedImageBitmap = bitmap,
                                selectedImageBase64 = base64,
                                selectedImageMime = "image/jpeg",
                                errorMessage = null
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(errorMessage = "Could not decode captured photo.") }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "Failed to process photo: ${e.localizedMessage}") }
                }
            }
        }
    }

    fun clearSelectedImage() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                selectedImageBitmap = null,
                selectedImageBase64 = null,
                selectedImageMime = null
            )
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
        forceOffline: Boolean = false,
        imageData: String? = null,
        imageMime: String? = null,
        imageUri: String? = null,
        imageBitmap: Bitmap? = null
    ) {
        val currentState = _uiState.value
        val effectiveImageData = imageData ?: currentState.selectedImageBase64
        val effectiveImageMime = imageMime ?: currentState.selectedImageMime ?: "image/jpeg"
        val effectiveImageUri = imageUri ?: currentState.selectedImageUri
        val effectiveImageBitmap = imageBitmap ?: currentState.selectedImageBitmap

        val q = question.trim()
        val effectivePrompt = if (q.isBlank() && !effectiveImageData.isNullOrBlank()) {
            "Please analyze this image and explain it clearly step by step."
        } else {
            q
        }

        if (effectivePrompt.isBlank()) return
        if (currentState.isLoading) return // Prevent duplicate requests while loading

        // Add user message to UI immediately
        val userMsg = QaMessageItem(
            sender = MessageSource.USER,
            text = effectivePrompt,
            imageBitmap = effectiveImageBitmap,
            imageUri = effectiveImageUri,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                messages = listOf(userMsg) + it.messages,
                isLoading = true,
                errorMessage = null,
                selectedImageUri = null,
                selectedImageBitmap = null,
                selectedImageBase64 = null,
                selectedImageMime = null
            )
        }

        val hasLectureContext = transcriptText.isNotBlank() || detailedNotes != null || summaryText.isNotBlank() || keyTakeaways.isNotEmpty()

        if (forceOffline) {
            executeOfflineAnswer(effectivePrompt, transcriptText, detailedNotes, summaryText, keyTakeaways, subject, lectureTitle)
            return
        }

        // Avoid repeatedly retrying Gemini when HTTP 429 rate limit is currently active
        if (System.currentTimeMillis() < geminiRateLimitedUntil) {
            executeOfflineAnswer(effectivePrompt, transcriptText, detailedNotes, summaryText, keyTakeaways, subject, lectureTitle)
            return
        }

        viewModelScope.launch {
            val currentConvId = _uiState.value.conversationId
            var assistantMessageId: String? = null
            var accumulatedText = ""
            var hasReceivedChunk = false

            try {
                ChatRepository.streamMessage(
                    message = effectivePrompt,
                    conversationId = currentConvId,
                    subject = subject,
                    lectureTitle = lectureTitle,
                    imageData = effectiveImageData,
                    imageMime = effectiveImageMime
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Start -> {
                            _uiState.update { state ->
                                state.copy(
                                    conversationId = event.conversationId,
                                    conversationTitle = event.conversationTitle.ifBlank { state.conversationTitle }
                                )
                            }
                        }

                        is ChatStreamEvent.Chunk -> {
                            hasReceivedChunk = true
                            accumulatedText += event.chunkText

                            if (assistantMessageId == null) {
                                // Create exactly ONE assistant message on the first chunk
                                val newAssistantMsg = QaMessageItem(
                                    sender = MessageSource.ONLINE_GEMINI,
                                    text = accumulatedText,
                                    provider = "SmartNotes AI",
                                    isAnswered = true,
                                    isStreaming = true,
                                    timestamp = System.currentTimeMillis()
                                )
                                assistantMessageId = newAssistantMsg.id

                                _uiState.update { state ->
                                    state.copy(
                                        messages = listOf(newAssistantMsg) + state.messages,
                                        isLoading = false,
                                        errorMessage = null
                                    )
                                }
                            } else {
                                // Update that same assistant message in-place as more chunks arrive
                                val targetId = assistantMessageId!!
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages.map { msg ->
                                            if (msg.id == targetId) {
                                                msg.copy(text = accumulatedText, isStreaming = true)
                                            } else {
                                                msg
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        is ChatStreamEvent.Done -> {
                            val targetId = assistantMessageId
                            if (targetId != null) {
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages.map { msg ->
                                            if (msg.id == targetId) {
                                                msg.copy(
                                                    text = accumulatedText,
                                                    isStreaming = false,
                                                    isAnswered = true
                                                )
                                            } else {
                                                msg
                                            }
                                        },
                                        conversationId = event.conversationId,
                                        conversationTitle = event.conversationTitle.ifBlank { state.conversationTitle },
                                        isLoading = false,
                                        errorMessage = null
                                    )
                                }
                            } else {
                                // Insert completed assistant message into messages list even if no Chunk event was received
                                if (accumulatedText.isNotBlank()) {
                                    val completedMsg = QaMessageItem(
                                        sender = MessageSource.ONLINE_GEMINI,
                                        text = accumulatedText,
                                        provider = "SmartNotes AI",
                                        isAnswered = true,
                                        isStreaming = false,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    _uiState.update { state ->
                                        state.copy(
                                            messages = listOf(completedMsg) + state.messages,
                                            conversationId = event.conversationId,
                                            conversationTitle = event.conversationTitle.ifBlank { state.conversationTitle },
                                            isLoading = false,
                                            errorMessage = null
                                        )
                                    }
                                } else {
                                    _uiState.update { state ->
                                        state.copy(
                                            conversationId = event.conversationId,
                                            conversationTitle = event.conversationTitle.ifBlank { state.conversationTitle },
                                            isLoading = false
                                        )
                                    }
                                }
                            }

                            // Auto-save to My Notes ONLY after a valid Done event with complete answer
                            if (accumulatedText.isNotBlank()) {
                                autoSaveQuestionAnswerToMyNotes(
                                    question = effectivePrompt,
                                    answer = accumulatedText,
                                    subject = subject,
                                    lectureTitle = lectureTitle
                                )
                            }

                            // Refresh conversation list after successful completion
                            fetchConversations()
                        }

                        is ChatStreamEvent.Error -> {
                            val isRateLimitError = event.errorCode?.contains("429") == true || event.message.contains("429")
                            if (isRateLimitError) {
                                // Cooldown to avoid repeatedly slamming Gemini when 429 is returned
                                geminiRateLimitedUntil = System.currentTimeMillis() + 60_000L
                            }

                            if (!hasReceivedChunk && assistantMessageId == null) {
                                // Error occurred BEFORE any chunks: trigger existing offline fallback
                                val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
                                    question = effectivePrompt,
                                    transcriptText = transcriptText,
                                    detailedNotes = detailedNotes,
                                    summaryText = summaryText,
                                    keyTakeaways = keyTakeaways
                                )

                                val friendlyProvider = if (hasLectureContext) {
                                    "Offline Curriculum"
                                } else {
                                    "Answered using Offline Knowledge"
                                }

                                if (offlineAnswer.isAnswered) {
                                    // Fallback successfully generated an answer:
                                    // Keep and display answer, friendly label, and DO NOT show red error toast
                                    val offlineMsg = QaMessageItem(
                                        sender = MessageSource.OFFLINE_ENGINE,
                                        text = offlineAnswer.answer,
                                        explanation = offlineAnswer.explanation,
                                        provider = friendlyProvider,
                                        isAnswered = true,
                                        isStreaming = false,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    _uiState.update { state ->
                                        state.copy(
                                            messages = listOf(offlineMsg) + state.messages,
                                            isLoading = false,
                                            errorMessage = null // Do not show red error banner
                                        )
                                    }
                                    autoSaveQuestionAnswerToMyNotes(
                                        question = effectivePrompt,
                                        answer = offlineAnswer.answer,
                                        subject = subject,
                                        lectureTitle = lectureTitle
                                    )
                                } else {
                                    // Both Gemini and fallback failed:
                                    // Show clear friendly error message and always stop loading indicator
                                    val displayText = if (!hasLectureContext) {
                                        "SmartNotes AI is currently unable to answer this question. Live AI service is temporarily unavailable and no matching explanation was found in offline knowledge."
                                    } else {
                                        offlineAnswer.answer
                                    }

                                    val offlineMsg = QaMessageItem(
                                        sender = MessageSource.OFFLINE_ENGINE,
                                        text = displayText,
                                        explanation = offlineAnswer.explanation,
                                        provider = friendlyProvider,
                                        isAnswered = false,
                                        isStreaming = false,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    val friendlyError = if (isRateLimitError) {
                                        "Live AI service is temporarily busy (rate limited). Please try again in a few moments."
                                    } else {
                                        event.message.ifBlank { "AI service is temporarily unavailable. Please try again shortly." }
                                    }

                                    _uiState.update { state ->
                                        state.copy(
                                            messages = listOf(offlineMsg) + state.messages,
                                            isLoading = false,
                                            errorMessage = friendlyError
                                        )
                                    }
                                }
                            } else {
                                // Error occurred AFTER partial chunks:
                                val targetId = assistantMessageId
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages.map { msg ->
                                            if (msg.id == targetId) {
                                                msg.copy(
                                                    isStreaming = false,
                                                    isAnswered = false,
                                                    explanation = "(Stream interrupted: ${event.message})"
                                                )
                                            } else {
                                                msg
                                            }
                                        },
                                        isLoading = false,
                                        errorMessage = if (isRateLimitError) {
                                            "Live AI service is temporarily busy (rate limited). Please try again in a few moments."
                                        } else {
                                            event.message
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errText = e.localizedMessage ?: "Network error"
                val isRateLimitError = errText.contains("429")
                if (isRateLimitError) {
                    geminiRateLimitedUntil = System.currentTimeMillis() + 60_000L
                }

                if (!hasReceivedChunk && assistantMessageId == null) {
                    val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
                        question = effectivePrompt,
                        transcriptText = transcriptText,
                        detailedNotes = detailedNotes,
                        summaryText = summaryText,
                        keyTakeaways = keyTakeaways
                    )

                    val friendlyProvider = if (hasLectureContext) {
                        "Offline Curriculum"
                    } else {
                        "Answered using Offline Knowledge"
                    }

                    if (offlineAnswer.isAnswered) {
                        val offlineMsg = QaMessageItem(
                            sender = MessageSource.OFFLINE_ENGINE,
                            text = offlineAnswer.answer,
                            explanation = offlineAnswer.explanation,
                            provider = friendlyProvider,
                            isAnswered = true,
                            isStreaming = false,
                            timestamp = System.currentTimeMillis()
                        )
                        _uiState.update { state ->
                            state.copy(
                                messages = listOf(offlineMsg) + state.messages,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        autoSaveQuestionAnswerToMyNotes(
                            question = effectivePrompt,
                            answer = offlineAnswer.answer,
                            subject = subject,
                            lectureTitle = lectureTitle
                        )
                    } else {
                        val displayText = if (!hasLectureContext) {
                            "SmartNotes AI is currently unable to answer this question. Live AI service is temporarily unavailable and no matching explanation was found in offline knowledge."
                        } else {
                            offlineAnswer.answer
                        }
                        val offlineMsg = QaMessageItem(
                            sender = MessageSource.OFFLINE_ENGINE,
                            text = displayText,
                            explanation = offlineAnswer.explanation,
                            provider = friendlyProvider,
                            isAnswered = false,
                            isStreaming = false,
                            timestamp = System.currentTimeMillis()
                        )
                        val friendlyError = if (isRateLimitError) {
                            "Live AI service is temporarily busy (rate limited). Please try again in a few moments."
                        } else {
                            errText
                        }
                        _uiState.update { state ->
                            state.copy(
                                messages = listOf(offlineMsg) + state.messages,
                                isLoading = false,
                                errorMessage = friendlyError
                            )
                        }
                    }
                }
            } finally {
                // Guaranteed safety: ensure isLoading is never left stuck as true
                _uiState.update { state ->
                    if (state.isLoading) {
                        state.copy(isLoading = false)
                    } else {
                        state
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
        keyTakeaways: List<String>,
        subject: String? = null,
        lectureTitle: String? = null
    ) {
        val hasLectureContext = transcriptText.isNotBlank() || detailedNotes != null || summaryText.isNotBlank() || keyTakeaways.isNotEmpty()
        val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
            question = question,
            transcriptText = transcriptText,
            detailedNotes = detailedNotes,
            summaryText = summaryText,
            keyTakeaways = keyTakeaways
        )

        val friendlyProvider = if (hasLectureContext) {
            "Offline Curriculum"
        } else {
            "Answered using Offline Knowledge"
        }

        val displayText = if (!hasLectureContext && !offlineAnswer.isAnswered) {
            "SmartNotes AI is currently unable to answer this question. Live AI service is temporarily unavailable and no matching explanation was found in offline knowledge."
        } else {
            offlineAnswer.answer
        }

        val offlineMsg = QaMessageItem(
            sender = MessageSource.OFFLINE_ENGINE,
            text = displayText,
            explanation = offlineAnswer.explanation,
            provider = friendlyProvider,
            isAnswered = offlineAnswer.isAnswered,
            timestamp = System.currentTimeMillis()
        )

        val errorMsg = if (!offlineAnswer.isAnswered) {
            "Live AI service is temporarily unavailable. Please try again in a few moments."
        } else {
            null
        }

        _uiState.update {
            it.copy(
                messages = listOf(offlineMsg) + it.messages,
                isLoading = false,
                errorMessage = errorMsg
            )
        }
        if (offlineAnswer.isAnswered) {
            autoSaveQuestionAnswerToMyNotes(question, offlineAnswer.answer, subject, lectureTitle)
        }
    }

    private fun autoSaveQuestionAnswerToMyNotes(
        question: String,
        answer: String,
        subject: String?,
        lectureTitle: String?
    ) {
        val q = question.trim()
        val a = answer.trim()
        if (q.isBlank() || a.isBlank()) return

        val s = subject?.ifBlank { "Computer Science" } ?: "Computer Science"
        val l = lectureTitle?.ifBlank { "AI Study Copilot" } ?: "AI Study Copilot"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.Simtas.data.repository.NotesRepositoryImpl.saveAiQuestionAnswer(
                    question = q,
                    answer = a,
                    subject = s,
                    lectureTitle = l
                )
            } catch (_: Exception) {}
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
                        val firstAttach = if (isUser) msg.attachments.firstOrNull() else null
                        val cachedBitmap = firstAttach?.id?.let { ChatRepository.getCachedAttachmentBitmap(it) }
                        QaMessageItem(
                            sender = if (isUser) MessageSource.USER else MessageSource.ONLINE_GEMINI,
                            text = msg.message_content,
                            provider = msg.provider ?: if (isUser) null else "gemini",
                            timestamp = System.currentTimeMillis() - (result.data.messages.size - index) * 1000L,
                            attachmentId = firstAttach?.id,
                            imageBitmap = cachedBitmap,
                            isImageLoading = firstAttach != null && cachedBitmap == null,
                            imageLoadFailed = false
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

                    // Asynchronously load images for attachments not yet cached
                    historyItems.filter { it.attachmentId != null && it.imageBitmap == null }.forEach { item ->
                        viewModelScope.launch {
                            val attachId = item.attachmentId ?: return@launch
                            val bitmap = ChatRepository.getAttachmentBitmap(attachId)
                            _uiState.update { state ->
                                val updatedMessages = state.messages.map { m ->
                                    if (m.id == item.id) {
                                        if (bitmap != null) {
                                            m.copy(imageBitmap = bitmap, isImageLoading = false, imageLoadFailed = false)
                                        } else {
                                            m.copy(isImageLoading = false, imageLoadFailed = true)
                                        }
                                    } else {
                                        m
                                    }
                                }
                                state.copy(messages = updatedMessages)
                            }
                        }
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

    fun deleteConversation(conversationId: Int, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = ChatRepository.deleteConversation(conversationId)
            when (result) {
                is ChatResult.Success -> {
                    if (_uiState.value.conversationId == conversationId) {
                        startNewChat()
                    }
                    fetchConversations()
                    onDeleted?.invoke()
                }
                is ChatResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startNewConversation() {
        startNewChat()
    }
}
