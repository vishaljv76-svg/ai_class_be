package com.example.Simtas.ui.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.ml.OfflineSpeechRecognizer
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.transcript.TranscriptParagraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for [LecturePreviewScreen] adhering to MVVM architecture.
 * Manages audio playback, offline speech-to-text recognition via Sherpa-ONNX,
 * real-time microphone voice input (ChatGPT style), and manual lecture text editing.
 */
class LecturePreviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LecturePreviewUiState())
    val uiState: StateFlow<LecturePreviewUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentSpeed: Float = 1.0f

    // Voice input recorder for Screen 2 mic input
    private var voiceRecorder: MediaRecorder? = null
    private var currentVoiceFile: File? = null
    private val voiceLock = Any()

    init {
        loadAudioFromSession()
    }

    fun loadAudio(path: String? = null, context: Context? = null) {
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                applyAudioFile(file, context)
                return
            }
        }

        // Try SessionManager
        val sessionPath = SessionManager.getLastRecordedAudioPath()
        if (!sessionPath.isNullOrBlank()) {
            val file = File(sessionPath)
            if (file.exists() && file.length() > 0L) {
                applyAudioFile(file, context)
                return
            }
        }

        // Check private recordings directory
        if (context != null) {
            val recDir = File(context.filesDir, "recordings")
            if (recDir.exists()) {
                val latest = recDir.listFiles()
                    ?.filter { it.extension.equals("m4a", ignoreCase = true) || it.extension.equals("wav", ignoreCase = true) }
                    ?.maxByOrNull { it.lastModified() }
                if (latest != null && latest.length() > 0L) {
                    applyAudioFile(latest, context)
                    return
                }
            }
        }

        // Check if text already exists in session
        val cachedText = SessionManager.getLastTranscriptionText()
        if (!cachedText.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    recognizedText = cachedText,
                    statusText = "Text loaded from session"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    statusText = "Speak with mic or type lecture notes below",
                    fileMetadataText = "Tap mic to speak or enter text",
                    hasAudioFile = false
                )
            }
        }
    }

    private fun loadAudioFromSession() {
        val lastTitle = SessionManager.getLastUploadedAudioTitle()
        if (!lastTitle.isNullOrBlank()) {
            _uiState.update { it.copy(lectureTitle = lastTitle) }
        }

        val lastDuration = SessionManager.getLastUploadedAudioDuration()
        if (!lastDuration.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    totalTimeText = lastDuration,
                    dateDurationCodeText = "Recorded · $lastDuration · CS301"
                )
            }
        }

        val sessionPath = SessionManager.getLastRecordedAudioPath()
        if (!sessionPath.isNullOrBlank()) {
            val file = File(sessionPath)
            if (file.exists() && file.length() > 0L) {
                applyAudioFile(file)
            }
        }

        val sessionText = SessionManager.getLastTranscriptionText()
        if (!sessionText.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    recognizedText = sessionText,
                    statusText = "Text ready"
                )
            }
            analyzeAndAnswerQuestions(sessionText)
        }
    }

    private fun applyAudioFile(file: File, context: Context? = null) {
        SessionManager.setLastRecordedAudioPath(file.absolutePath)
        val sizeFormatted = formatBytes(file.length())
        val detectedDuration = extractAudioDuration(file) ?: _uiState.value.totalTimeText.ifBlank { "00:00" }

        val defaultTitle = _uiState.value.lectureTitle.ifBlank {
            file.nameWithoutExtension.replace('_', ' ').capitalizeWords()
        }

        SessionManager.setLastUploadedAudio(
            audioId = SessionManager.getLastUploadedAudioId() ?: 0,
            title = defaultTitle,
            duration = detectedDuration
        )

        _uiState.update {
            it.copy(
                audioFilePath = file.absolutePath,
                audioFileName = file.name,
                fileSizeBytes = file.length(),
                hasAudioFile = true,
                totalTimeText = detectedDuration,
                lectureTitle = defaultTitle,
                dateDurationCodeText = "Recorded · $detectedDuration · CS301",
                fileMetadataText = "${file.name} · $sizeFormatted · Audio ready",
                statusText = "Ready · Audio loaded",
                errorMessage = null
            )
        }

        // Auto-transcribe recording if text is empty and context provided
        if (context != null && _uiState.value.recognizedText.isBlank()) {
            val cachedText = SessionManager.getLastTranscriptionText()
            if (!cachedText.isNullOrBlank()) {
                _uiState.update { it.copy(recognizedText = cachedText, statusText = "Speech recognized") }
                analyzeAndAnswerQuestions(cachedText)
            } else {
                transcribeLoadedAudio(context)
            }
        }
    }

    // --- Voice Input (Microphone on Screen 2) ---

    fun startVoiceInput(context: Context) {
        synchronized(voiceLock) {
            if (_uiState.value.isListening || voiceRecorder != null) return

            // Permission check
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                _uiState.update {
                    it.copy(errorMessage = "Microphone permission required for voice input.")
                }
                return
            }

            // Pause playback if playing
            if (_uiState.value.isPlaying) {
                pauseAudio()
            }

            try {
                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) recordingsDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(recordingsDir, "voice_input_${timestamp}.m4a")
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
                        statusText = "Listening... Speak clearly",
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

    fun stopVoiceInput(context: Context) {
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
                transcribeVoiceFile(context, file)
            } else {
                _uiState.update {
                    it.copy(
                        statusText = "No speech detected",
                        errorMessage = "Recording was too short. Tap mic to speak again."
                    )
                }
            }
        }
    }

    private fun transcribeVoiceFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isTranscribing = true,
                    statusText = "Recognizing spoken words...",
                    transcriptionProgress = 25
                )
            }

            val lang = _uiState.value.selectedLanguageCode
            val result = OfflineSpeechRecognizer.transcribeAudioFile(
                context = context,
                audioFile = file,
                languageCode = lang,
                onProgress = { p ->
                    _uiState.update { it.copy(transcriptionProgress = p) }
                }
            )

            if (result.isSuccess && result.fullTranscript.isNotBlank()) {
                val current = _uiState.value.recognizedText.trim()
                val newSpeech = result.fullTranscript.trim()
                val combined = if (current.isEmpty()) newSpeech else "$current\n$newSpeech"

                val paragraphs = buildParagraphsFromText(combined)
                SessionManager.setLastTranscription(combined, paragraphs)

                _uiState.update {
                    it.copy(
                        recognizedText = combined,
                        isTranscribing = false,
                        statusText = "Recognized (${if (lang == "ta") "Tamil" else "English"})",
                        errorMessage = null,
                        isSaved = true
                    )
                }
                analyzeAndAnswerQuestions(combined)
            } else {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        statusText = "Recognition complete",
                        errorMessage = result.errorMessage ?: "Could not detect clear words. Try speaking closer to mic."
                    )
                }
            }
        }
    }

    fun transcribeLoadedAudio(context: Context) {
        val path = _uiState.value.audioFilePath ?: SessionManager.getLastRecordedAudioPath() ?: return
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isTranscribing = true,
                    statusText = "Transcribing lecture audio...",
                    transcriptionProgress = 15
                )
            }

            val lang = _uiState.value.selectedLanguageCode
            val result = OfflineSpeechRecognizer.transcribeAudioFile(
                context = context,
                audioFile = file,
                languageCode = lang,
                onProgress = { p ->
                    _uiState.update { it.copy(transcriptionProgress = p) }
                }
            )

            if (result.isSuccess && result.fullTranscript.isNotBlank()) {
                val current = _uiState.value.recognizedText.trim()
                val newSpeech = result.fullTranscript.trim()
                val combined = if (current.isEmpty()) newSpeech else "$current\n$newSpeech"

                val paragraphs = if (result.paragraphs.isNotEmpty()) result.paragraphs else buildParagraphsFromText(combined)
                SessionManager.setLastTranscription(combined, paragraphs)

                _uiState.update {
                    it.copy(
                        recognizedText = combined,
                        isTranscribing = false,
                        statusText = "Speech recognized",
                        errorMessage = null,
                        isSaved = true
                    )
                }
                analyzeAndAnswerQuestions(combined)
            } else {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        statusText = "Ready to speak or type",
                        errorMessage = result.errorMessage ?: "No speech recognized. You can type notes manually."
                    )
                }
            }
        }
    }

    // --- Manual Text Input, Question Answering & Language Options ---

    private var questionAnsweringJob: Job? = null

    fun onRecognizedTextChanged(newText: String) {
        _uiState.update { it.copy(recognizedText = newText, errorMessage = null) }
        val paragraphs = buildParagraphsFromText(newText)
        SessionManager.setLastTranscription(newText, paragraphs)
        analyzeAndAnswerQuestions(newText)
    }

    fun onClearTextClicked() {
        questionAnsweringJob?.cancel()
        _uiState.update {
            it.copy(
                recognizedText = "",
                detectedQuestionsAndAnswers = emptyList(),
                isAnsweringQuestion = false,
                errorMessage = null
            )
        }
        SessionManager.setLastTranscription("", emptyList())
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
    }

    fun onLanguageSelected(code: String) {
        _uiState.update { it.copy(selectedLanguageCode = code) }
    }

    /**
     * Reactively extracts questions and generates answers using QuestionAnsweringEngine.
     */
    fun analyzeAndAnswerQuestions(text: String) {
        questionAnsweringJob?.cancel()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    detectedQuestionsAndAnswers = emptyList(),
                    isAnsweringQuestion = false
                )
            }
            SessionManager.setDetectedQuestionsAndAnswers(emptyList())
            return
        }

        questionAnsweringJob = viewModelScope.launch(Dispatchers.Default) {
            val questions = com.example.Simtas.data.ml.QuestionDetector.extractQuestions(trimmed)
            if (questions.isEmpty()) {
                _uiState.update {
                    it.copy(
                        detectedQuestionsAndAnswers = emptyList(),
                        isAnsweringQuestion = false
                    )
                }
                SessionManager.setDetectedQuestionsAndAnswers(emptyList())
                return@launch
            }

            _uiState.update { it.copy(isAnsweringQuestion = true) }

            val qaItems = mutableListOf<com.example.Simtas.ui.notes.DetectedQuestionAnswerItem>()
            for (q in questions) {
                val item = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(
                    question = q,
                    transcriptText = trimmed,
                    detailedNotes = SessionManager.getLastDetailedNotes(),
                    summaryText = SessionManager.getLastGeneratedNote()?.aiSummaryText ?: "",
                    keyTakeaways = SessionManager.getLastGeneratedNote()?.keyTakeaways ?: emptyList()
                )
                qaItems.add(item)
            }

            SessionManager.setDetectedQuestionsAndAnswers(qaItems)
            _uiState.update {
                it.copy(
                    detectedQuestionsAndAnswers = qaItems,
                    isAnsweringQuestion = false
                )
            }
        }
    }

    private fun buildParagraphsFromText(text: String): List<TranscriptParagraph> {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return if (text.isNotBlank()) {
                listOf(TranscriptParagraph(timestamp = "00:00", speaker = "Speaker", text = text.trim()))
            } else emptyList()
        }
        return lines.mapIndexed { idx, line ->
            val sec = idx * 10
            val timestamp = String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
            TranscriptParagraph(timestamp = timestamp, speaker = "Speaker", text = line)
        }
    }

    // --- Lecture Metadata Updates ---

    fun onLectureTitleChanged(title: String) {
        _uiState.update { it.copy(lectureTitle = title) }
        SessionManager.setLastUploadedAudio(
            audioId = SessionManager.getLastUploadedAudioId() ?: 0,
            title = title,
            duration = _uiState.value.totalTimeText
        )
    }

    fun onSubjectSelected(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    // --- Audio Playback Controls ---

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pauseAudio()
        } else {
            playAudio()
        }
    }

    private fun playAudio() {
        val path = _uiState.value.audioFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            _uiState.update { it.copy(errorMessage = "Audio file not found on disk") }
            return
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        stopProgressTracker()
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                currentTimeText = "00:00",
                                playbackProgress = 0f
                            )
                        }
                    }
                }
            }

            applyPlaybackSpeed(currentSpeed)
            mediaPlayer?.start()
            _uiState.update { it.copy(isPlaying = true, errorMessage = null) }
            startProgressTracker()

        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = "Playback error: ${e.localizedMessage ?: "Unable to play audio"}"
                )
            }
        }
    }

    private fun pauseAudio() {
        try {
            mediaPlayer?.pause()
            stopProgressTracker()
            _uiState.update { it.copy(isPlaying = false) }
        } catch (_: Exception) {}
    }

    fun onRewind10s() {
        val player = mediaPlayer ?: return
        try {
            val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
            player.seekTo(newPos)
            updatePlaybackTime(newPos, player.duration)
        } catch (_: Exception) {}
    }

    fun onForward10s() {
        val player = mediaPlayer ?: return
        try {
            val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
            player.seekTo(newPos)
            updatePlaybackTime(newPos, player.duration)
        } catch (_: Exception) {}
    }

    fun onSpeedClicked() {
        val (nextSpeed, label) = when (_uiState.value.playbackSpeedText) {
            "1x" -> 1.25f to "1.25x"
            "1.25x" -> 1.5f to "1.5x"
            "1.5x" -> 2.0f to "2x"
            else -> 1.0f to "1x"
        }
        currentSpeed = nextSpeed
        applyPlaybackSpeed(nextSpeed)
        _uiState.update { it.copy(playbackSpeedText = label) }
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().apply { this.speed = speed }
            } catch (_: Exception) {}
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val current = player.currentPosition
                    val duration = player.duration
                    updatePlaybackTime(current, duration)
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlaybackTime(currentMs: Int, totalMs: Int) {
        val curSec = currentMs / 1000
        val totSec = (totalMs / 1000).coerceAtLeast(1)
        val curTimeStr = String.format(Locale.US, "%02d:%02d", curSec / 60, curSec % 60)
        val progress = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        _uiState.update {
            it.copy(
                currentTimeText = curTimeStr,
                playbackProgress = progress
            )
        }
    }

    fun onGenerateTranscriptClicked(): Boolean {
        pauseAudio()
        releaseVoiceRecorder()

        val text = _uiState.value.recognizedText.trim()
        val path = _uiState.value.audioFilePath ?: SessionManager.getLastRecordedAudioPath()

        if (text.isBlank() && (path.isNullOrBlank() || !File(path).exists())) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please speak using the mic or type lecture notes before continuing."
                )
            }
            return false
        }

        // Ensure current text, title, and detected Q&As are saved to SessionManager for Transcript & Notes screens
        val currentTitle = _uiState.value.lectureTitle
        val resolvedTitle = com.example.Simtas.data.ml.LectureNotesGenerator.resolveLectureTitle(currentTitle, text)
        SessionManager.setLastUploadedAudio(
            audioId = SessionManager.getLastUploadedAudioId() ?: 0,
            title = resolvedTitle,
            duration = _uiState.value.totalTimeText
        )
        val paragraphs = buildParagraphsFromText(text)
        SessionManager.setLastTranscription(text, paragraphs)
        SessionManager.setDetectedQuestionsAndAnswers(_uiState.value.detectedQuestionsAndAnswers)
        return true
    }

    fun onScreenDisposed() {
        releasePlayer()
        releaseVoiceRecorder()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        releaseVoiceRecorder()
    }

    private fun releasePlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun releaseVoiceRecorder() {
        synchronized(voiceLock) {
            try {
                voiceRecorder?.stop()
                voiceRecorder?.reset()
                voiceRecorder?.release()
            } catch (_: Exception) {}
            voiceRecorder = null
            _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun extractAudioDuration(file: File): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (durationMs != null && durationMs > 0) {
                val totalSec = durationMs / 1000
                String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
            } else null
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }
}
