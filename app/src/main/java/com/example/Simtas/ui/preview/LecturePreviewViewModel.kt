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
import com.example.Simtas.data.local.LectureNotesStorage
import com.example.Simtas.data.ml.OfflineSpeechRecognizer
import com.example.Simtas.data.ml.WhisperModelManager
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AudioRepository
import com.example.Simtas.data.repository.ChatRepository
import com.example.Simtas.data.repository.ChatResult
import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import com.example.Simtas.ui.transcript.TranscriptParagraph
import org.json.JSONArray
import org.json.JSONObject
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

    companion object {
        private val CONVERSATIONAL_EXCLUSIONS = setOf(
            "students", "student", "today", "tomorrow", "yesterday", "teacher", "professor",
            "class", "classes", "everyone", "anybody", "somebody", "nobody", "guys", "morning",
            "afternoon", "evening", "hello", "hi", "welcome", "alright", "okay", "please",
            "lecture", "topic", "session", "lesson", "example", "examples", "thing", "things",
            "something", "anything", "nothing", "here", "there", "now", "well", "yes", "no",
            "good", "great", "fine", "thanks", "thank", "you", "we", "they", "our", "their",
            "people", "person", "question", "questions", "answer", "answers"
        )

        private val TAMIL_CONVERSATIONAL_EXCLUSIONS = setOf(
            "வணக்கம்", "மாணவர்கள்", "மாணவர்களே", "இன்று", "நாளை", "நேற்று", 
            "ஆசிரியர்", "வகுப்பு", "பாடம்", "அனைவருக்கும்", "நன்றி", "நாம்", "நீங்கள்"
        )
    }

    private val _uiState = MutableStateFlow(LecturePreviewUiState())
    val uiState: StateFlow<LecturePreviewUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentSpeed: Float = 1.0f

    // Voice input recorder for Screen 2 mic input
    private var voiceRecorder: MediaRecorder? = null
    private var currentVoiceFile: File? = null
    private val voiceLock = Any()

    // Cache to prevent duplicate Gemini calls for identical transcript & language
    private var lastAnalyzedText: String? = null
    private var lastAnalyzedLanguage: String? = null

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
        val savedLang = SessionManager.getSelectedLanguage()
        if (savedLang.isNotBlank()) {
            _uiState.update { it.copy(selectedLanguageCode = savedLang) }
        }

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

    // --- Voice Input for Follow-Up Questions (Screen 2) ---

    fun startFollowUpVoiceInput(context: Context) {
        synchronized(voiceLock) {
            if (_uiState.value.isFollowUpListening || voiceRecorder != null) return

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                _uiState.update {
                    it.copy(errorMessage = "Microphone permission required for voice question.")
                }
                return
            }

            if (_uiState.value.isPlaying) {
                pauseAudio()
            }

            try {
                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) recordingsDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(recordingsDir, "followup_voice_${timestamp}.m4a")
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
                        isFollowUpListening = true,
                        statusText = "Listening for your question...",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                releaseVoiceRecorder()
                _uiState.update {
                    it.copy(
                        isFollowUpListening = false,
                        errorMessage = "Microphone error: ${e.localizedMessage ?: "Unable to access microphone"}"
                    )
                }
            }
        }
    }

    fun stopFollowUpVoiceInput(context: Context) {
        synchronized(voiceLock) {
            val recorder = voiceRecorder
            if (recorder != null) {
                try { recorder.stop() } catch (_: Exception) {}
                try { recorder.reset() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
            }
            voiceRecorder = null
            _uiState.update { it.copy(isFollowUpListening = false) }

            val file = currentVoiceFile
            if (file != null && file.exists() && file.length() > 0L) {
                viewModelScope.launch(Dispatchers.Default) {
                    if (!ensureModelInstalled(context)) return@launch

                    val lang = _uiState.value.selectedLanguageCode
                    val result = OfflineSpeechRecognizer.transcribeAudioFile(
                        context = context,
                        audioFile = file,
                        languageCode = lang
                    )

                    if (result.isSuccess && result.fullTranscript.isNotBlank()) {
                        val spokenQ = result.fullTranscript.trim()
                        _uiState.update {
                            it.copy(
                                followUpQuestionText = spokenQ,
                                statusText = "Spoken question recognized"
                            )
                        }
                        askFollowUpQuestion(spokenQ)
                    } else {
                        _uiState.update {
                            it.copy(
                                statusText = "No question detected",
                                errorMessage = "Voice question was too short. Tap mic to speak again."
                            )
                        }
                    }
                }
            }
        }
    }

    private val downloadLock = Any()
    @Volatile
    private var isDownloadingModel = false

    /**
     * Ensures the offline Whisper model files (~99 MB) are installed before transcription.
     * If missing, downloads all required model files with progress reporting.
     * Prevents duplicate simultaneous downloads and runs asynchronously.
     */
    private suspend fun ensureModelInstalled(context: Context): Boolean {
        if (WhisperModelManager.isModelInstalled(context)) {
            return true
        }

        synchronized(downloadLock) {
            if (isDownloadingModel) {
                _uiState.update {
                    it.copy(
                        isTranscribing = true,
                        statusText = "Downloading speech model in progress...",
                        errorMessage = null
                    )
                }
                return false
            }
            isDownloadingModel = true
        }

        _uiState.update {
            it.copy(
                isTranscribing = true,
                statusText = "Downloading offline speech model (~99 MB)...",
                transcriptionProgress = 0,
                errorMessage = null
            )
        }

        val downloadResult = try {
            WhisperModelManager.downloadModel(context) { progress ->
                _uiState.update {
                    it.copy(
                        statusText = progress.statusMessage,
                        transcriptionProgress = progress.percentage
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
                    statusText = "Offline model ready! Transcribing speech...",
                    transcriptionProgress = 5,
                    errorMessage = null
                )
            }
            true
        } else {
            val err = downloadResult.exceptionOrNull()?.localizedMessage
                ?: "Failed to download offline speech model. Internet connection required for first-time setup."
            _uiState.update {
                it.copy(
                    isTranscribing = false,
                    statusText = "Model download required",
                    errorMessage = "$err Tap mic or transcribe to retry."
                )
            }
            false
        }
    }

    private fun transcribeVoiceFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!ensureModelInstalled(context)) {
                return@launch
            }

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
                        transcriptionProgress = 100,
                        transcriptionFailed = false,
                        canRetryTranscription = false,
                        statusText = "Recognized (${if (lang == "ta") "Tamil" else "English"})",
                        errorMessage = null,
                        isSaved = true
                    )
                }
                extractLectureIntelligence(combined, paragraphs)
            } else {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        transcriptionProgress = 0,
                        transcriptionFailed = true,
                        canRetryTranscription = true,
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
            if (!ensureModelInstalled(context)) {
                return@launch
            }

            _uiState.update {
                it.copy(
                    isTranscribing = true,
                    transcriptionFailed = false,
                    canRetryTranscription = false,
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
                        transcriptionProgress = 100,
                        transcriptionFailed = false,
                        canRetryTranscription = false,
                        statusText = "Speech recognized",
                        errorMessage = null,
                        isSaved = true
                    )
                }
                extractLectureIntelligence(combined, paragraphs)
            } else {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        transcriptionProgress = 0,
                        transcriptionFailed = true,
                        canRetryTranscription = true,
                        statusText = "Recognition unconfirmed",
                        errorMessage = result.errorMessage ?: "No speech recognized. Tap 'Retry Transcription' or type notes manually."
                    )
                }
            }
        }
    }

    // --- Manual Text Input, Question Answering & Language Options ---

    private var questionAnsweringJob: Job? = null
    private var intelligenceJob: Job? = null
    private var textDebounceJob: Job? = null

    // Analysis concurrency guards
    @Volatile
    private var isAnalysisInProgress = false
    private var inProgressText: String? = null
    private var inProgressLanguage: String? = null

    fun onRecognizedTextChanged(newText: String) {
        _uiState.update { it.copy(recognizedText = newText, errorMessage = null) }
        val paragraphs = buildParagraphsFromText(newText)
        SessionManager.setLastTranscription(newText, paragraphs)

        val trimmed = newText.trim()
        if (trimmed.isEmpty()) {
            onClearTextClicked()
            return
        }

        // 1. Immediately synthesize local extractive baseline so UI and offline fallback are instant
        val baseline = synthesizeExtractiveBaseline(trimmed, paragraphs)

        // 2. 600ms debounce for remote AI analysis to prevent rapid keystrokes from restarting network calls repeatedly
        textDebounceJob?.cancel()
        textDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            delay(600)
            invokeRemoteGeminiAnalysis(trimmed, paragraphs, baseline = baseline)
        }
    }

    fun onClearTextClicked() {
        textDebounceJob?.cancel()
        intelligenceJob?.cancel()
        questionAnsweringJob?.cancel()
        isAnalysisInProgress = false
        inProgressText = null
        inProgressLanguage = null
        _uiState.update {
            it.copy(
                recognizedText = "",
                detectedQuestionsAndAnswers = emptyList(),
                studyQuestionsAndAnswers = emptyList(),
                teacherExplanation = "",
                lectureSummary = "",
                keyTakeaways = emptyList(),
                importantDefinitions = emptyList(),
                isAnalyzingIntelligence = false,
                isAnsweringQuestion = false,
                canRetryAnalysis = false,
                analysisErrorMessage = null,
                errorMessage = null
            )
        }
        SessionManager.setLastTranscription("", emptyList())
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
        lastAnalyzedText = null
        lastAnalyzedLanguage = null
    }

    fun onLanguageSelected(code: String) {
        _uiState.update { it.copy(selectedLanguageCode = code) }
    }

    /**
     * Reactively analyzes lecture text, extracts key concepts, generates teacher's
     * explanation, summary, key definitions, and study questions strictly from the transcript.
     */
    fun analyzeAndAnswerQuestions(text: String) {
        extractLectureIntelligence(text)
    }

    private data class BaselineExtractionResult(
        val extractive: com.example.Simtas.data.ml.ExtractiveLectureNotesResult,
        val resolvedTitle: String,
        val detectedSubject: String,
        val teacherExplanation: String,
        val definitions: List<Pair<String, String>>,
        val baselineQuestions: List<DetectedQuestionAnswerItem>
    )

    private fun synthesizeExtractiveBaseline(
        trimmed: String,
        paragraphs: List<TranscriptParagraph>
    ): BaselineExtractionResult {
        val pList = if (paragraphs.isNotEmpty()) paragraphs else buildParagraphsFromText(trimmed)
        val currentLang = _uiState.value.selectedLanguageCode
        val isTamil = currentLang == "ta"

        // 1. Dynamically resolve real topic title and subject from transcript content
        val resolvedTitle = com.example.Simtas.data.ml.LectureNotesGenerator.resolveLectureTitle(_uiState.value.lectureTitle, trimmed)
        val (detectedSubject, courseCode) = com.example.Simtas.data.ml.LectureNotesGenerator.detectSubjectAndCourse(trimmed, resolvedTitle)
        val currentTitle = resolvedTitle
        val subjectStr = detectedSubject
        val durationStr = _uiState.value.totalTimeText

        // Update UI state with resolved title and subject immediately if changed
        if (resolvedTitle != _uiState.value.lectureTitle || detectedSubject != _uiState.value.selectedSubject) {
            _uiState.update {
                it.copy(
                    lectureTitle = resolvedTitle,
                    selectedSubject = detectedSubject,
                    subjectBadge = detectedSubject,
                    dateDurationCodeText = "Recorded · ${it.totalTimeText} · $courseCode"
                )
            }
            SessionManager.setLastUploadedAudio(
                audioId = SessionManager.getLastUploadedAudioId() ?: 0,
                title = resolvedTitle,
                duration = _uiState.value.totalTimeText
            )
        }

        val extractive = com.example.Simtas.data.ml.LectureNotesGenerator.generate(
            transcriptText = trimmed,
            paragraphs = pList,
            lectureTitle = currentTitle,
            subject = subjectStr,
            durationText = durationStr
        )

        val sec1 = extractive.detailedNotes.section1Content.trim()
        val calloutContent = extractive.detailedNotes.calloutContent.trim()
        val calloutTitle = extractive.detailedNotes.calloutTitle.trim()
        val sec3 = extractive.detailedNotes.section3Content1.trim()

        val defsList = mutableListOf<Pair<String, String>>()
        if (calloutTitle.isNotBlank() && calloutContent.isNotBlank()) {
            val cleanTitle = calloutTitle
                .removePrefix("Key Principle: ")
                .removePrefix("Core Concept: ")
                .removePrefix("Answer: ")
                .trim()
            val lowerClean = cleanTitle.lowercase(Locale.ROOT)
            val isExcluded = lowerClean in CONVERSATIONAL_EXCLUSIONS || 
                             com.example.Simtas.data.ml.LectureNotesGenerator.isConversationalFiller(lowerClean) ||
                             cleanTitle in TAMIL_CONVERSATIONAL_EXCLUSIONS ||
                             TAMIL_CONVERSATIONAL_EXCLUSIONS.any { cleanTitle.contains(it) } ||
                             (cleanTitle.equals("Database", ignoreCase = true) && !com.example.Simtas.data.ml.LectureNotesGenerator.isTopicAboutDatabaseNormalization(trimmed)) ||
                             calloutContent.trim().equals(trimmed, ignoreCase = true)
            if (!isExcluded && cleanTitle.length >= 3 && !calloutContent.trim().equals(trimmed, ignoreCase = true)) {
                defsList.add(cleanTitle to calloutContent)
            }
        }
        val defRegex = Regex("(?i)\\b([A-Z][a-zA-Z0-9\\s]{2,25})\\s+(is defined as|is the process of|refers to|means)\\s+([^.!?\\n]+)")
        for (match in defRegex.findAll(trimmed)) {
            val term = match.groupValues[1].trim()
            val lowerTerm = term.lowercase(Locale.ROOT)
            val defText = match.value.trim()
            val isExcluded = lowerTerm in CONVERSATIONAL_EXCLUSIONS || 
                             com.example.Simtas.data.ml.LectureNotesGenerator.isConversationalFiller(lowerTerm) ||
                             TAMIL_CONVERSATIONAL_EXCLUSIONS.any { term.contains(it) }
            if (term.isNotBlank() && !isExcluded && term.length >= 3 && defsList.none { it.first.equals(term, ignoreCase = true) }) {
                defsList.add(term to defText)
            }
        }
        val tamilDefRegex = Regex("""([A-Za-z0-9\u0B80-\u0BFF\s]{2,30}?)\s+என்பது\s+([^.!?\n]+)""")
        for (match in tamilDefRegex.findAll(trimmed)) {
            val term = match.groupValues[1].trim().lines().last().trim()
            val defText = match.value.trim()
            val isExcluded = TAMIL_CONVERSATIONAL_EXCLUSIONS.any { term.contains(it) }
            if (term.isNotBlank() && !isExcluded && term.length >= 3 && defsList.none { it.first == term }) {
                defsList.add(term to defText)
            }
        }

        val wordCount = trimmed.split("\\s+".toRegex()).count { it.isNotBlank() }
        val isShortIntro = wordCount < 35 || (defsList.isEmpty() && wordCount < 50)

        val teacherExp = if (isShortIntro) {
            if (isTamil) {
                "ஆசிரியர் $currentTitle பற்றிய அறிமுகத்தை வழங்கியுள்ளார். பதிவு செய்யப்பட்ட விரிவுரையில் ஆரம்ப அறிமுகம் மட்டுமே உள்ளதால், முழுமையான கல்வி விளக்கங்கள் மற்றும் விரிவான வினா-விடைகள் ஸ்மார்ட்நோட்ஸ் AI மூலம் கீழே வழங்கப்பட்டுள்ளன."
            } else {
                "The teacher introduced the lecture on $currentTitle. The recorded audio contains an initial introductory overview; comprehensive concepts, structural classifications, and detailed revision questions are supplemented below by SmartNotes AI to provide complete study notes."
            }
        } else {
            buildString {
                if (sec1.isNotBlank()) {
                    append(sec1)
                } else if (extractive.generatedNotes.aiSummaryText.isNotBlank()) {
                    append(extractive.generatedNotes.aiSummaryText)
                }
                if (calloutContent.isNotBlank() && !contains(calloutContent) && !calloutContent.equals(trimmed, ignoreCase = true)) {
                    append("\n\n• Core Concept ($calloutTitle): ").append(calloutContent)
                }
                if (sec3.isNotBlank() && sec3 != sec1 && !contains(sec3) && !sec3.equals(trimmed, ignoreCase = true)) {
                    append("\n\n• Teacher's Discussion: ").append(sec3)
                }
            }
        }

        val lectureSummary = if (isShortIntro) {
            if (isTamil) {
                "$currentTitle பாட அறிமுகம். விரிவுரையில் அறிவிக்கப்பட்ட தலைப்பிற்கான அடிப்படைக் கருத்துகள், வகைப்பாடுகள் மற்றும் நிஜ உலகப் பயன்பாடுகள் இங்கு தொகுக்கப்பட்டுள்ளன."
            } else {
                "Introduction to $currentTitle. The recorded transcript announces the session topic. Foundational concepts, types, and practical engineering applications have been synthesized for thorough exam revision."
            }
        } else {
            extractive.generatedNotes.aiSummaryText.ifBlank { trimmed }
        }

        val takeaways = if (isShortIntro) {
            if (isTamil) {
                listOf(
                    "பதிவு செய்யப்பட்ட விரிவுரையில் $currentTitle பாடம் அறிமுகப்படுத்தப்பட்டது.",
                    "$currentTitle என்பது $subjectStr பாடத்தின் மிக முக்கியமான அடித்தளமாகும்.",
                    "முக்கிய வகைப்பாடுகள் மற்றும் நிஜ உலக உதாரணங்கள் கீழே விரிவாக விளக்கப்பட்டுள்ளன."
                )
            } else {
                listOf(
                    "The recorded lecture introduces the study of $currentTitle.",
                    "$currentTitle is a fundamental core concept in $subjectStr.",
                    "Essential types, architectural properties, and practical applications are detailed below."
                )
            }
        } else {
            extractive.generatedNotes.keyTakeaways.ifEmpty { listOf(sec1) }
        }

        val finalDefs = if (defsList.isNotEmpty()) {
            defsList.take(5)
        } else if (currentTitle.equals("Data Structures", ignoreCase = true) || currentTitle.contains("தரவு அமைப்பு")) {
            if (isTamil) {
                listOf(
                    "தரவு அமைப்பு (Data Structure)" to "கணினி நினைவகத்தில் தரவை திறமையாக சேமிக்கவும், ஒழுங்கமைக்கவும், கையாளவும் பயன்படும் ஒரு சிறப்பு வடிவம்.",
                    "நேரியல் தரவு அமைப்பு (Linear)" to "உறுப்புகள் தொடர்ச்சியாக வரிசைப்படுத்தப்பட்டிருக்கும் தரவு அமைப்பு (எ.கா: Arrays, Stacks, Queues)."
                )
            } else {
                listOf(
                    "Data Structure" to "A specialized format for organizing, processing, retrieving, and storing data efficiently in computer memory.",
                    "Linear Data Structure" to "A structure where data elements are sequentially attached and traversed in single level order (e.g., Arrays, Stacks, Queues)."
                )
            }
        } else {
            listOf(currentTitle to "Fundamental concept covered during the lecture session in $subjectStr.")
        }

        // Synthesize 5 to 10 meaningful study questions with detailed educational answers
        val baselineStudyQAs = mutableListOf<DetectedQuestionAnswerItem>()

        if (isShortIntro) {
            // Question 1: From Lecture (strictly grounded in what the teacher announced)
            val q1 = if (isTamil) {
                DetectedQuestionAnswerItem(
                    question = "பதிவு செய்யப்பட்ட விரிவுரையில் ஆசிரியர் அறிமுகப்படுத்திய பாடம் எது?",
                    answer = "பதிவு செய்யப்பட்ட விரிவுரையின்படி, ஆசிரியர் $currentTitle பற்றிய விரிவுரையைத் தொடங்கினார்.",
                    explanation = "பதிவு செய்யப்பட்ட விரிவுரையிலிருந்து நேரடியாக எடுக்கப்பட்ட தகவல்.",
                    status = "Answered from Transcript",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            } else {
                DetectedQuestionAnswerItem(
                    question = "What topic was introduced by the teacher in the recorded lecture?",
                    answer = "According to the lecture recording, the teacher introduced the session on $currentTitle.",
                    explanation = "Extracted directly from the recorded lecture transcript.",
                    status = "Answered from Transcript",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            }
            baselineStudyQAs.add(q1)

            // Questions 2 to 7: Supplemental AI Educational Questions strictly about $currentTitle
            val supplementalQAs = generateSupplementalQuestionsForTopic(currentTitle, subjectStr, isTamil)
            baselineStudyQAs.addAll(supplementalQAs)
        } else {
            // Long transcript: Questions are extracted directly from lecture content
            for ((term, def) in defsList.take(2)) {
                val qText = if (isTamil) "$term என்றால் என்ன? விரிவாக விளக்குக." else "What is $term according to the lecture?"
                val ansText = if (def.length > 50) def else "$def This fundamental concept was explained by the teacher as part of the core lecture topic."
                if (baselineStudyQAs.none { it.question.contains(term, ignoreCase = true) }) {
                    baselineStudyQAs.add(
                        DetectedQuestionAnswerItem(
                            question = qText,
                            answer = ansText,
                            explanation = "Concept definition extracted directly from the lecture recording.",
                            status = "Answered from Transcript",
                            isAnswered = true,
                            source = "[From Lecture]"
                        )
                    )
                }
            }

            val typesQ = if (isTamil) "$currentTitle பாடத்தில் குறிப்பிடப்பட்ட வகைகள் யாவை?" else "Explain the types and classifications of $currentTitle discussed in the lecture."
            val typesBullets = extractive.detailedNotes.section2Bullets.filter { it.isNotBlank() }
            val typesAns = if (typesBullets.isNotEmpty()) {
                "The lecture identified the following core categories:\n• " + typesBullets.joinToString("\n• ")
            } else if (extractive.generatedNotes.keyTakeaways.isNotEmpty()) {
                "Key classifications from the lecture include:\n• " + extractive.generatedNotes.keyTakeaways.take(3).joinToString("\n• ")
            } else {
                "The teacher outlined the core structural variants and principles governing $currentTitle as covered during the session."
            }
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = typesQ,
                    answer = typesAns,
                    explanation = "Types and structural categories analyzed from teacher's lecture.",
                    status = "Answered from Lecture Notes",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )

            val charQ = if (isTamil) "$currentTitle பாடத்தின் முக்கிய பண்புகள் மற்றும் விதிகள் யாவை?" else "What are the essential characteristics and properties of $currentTitle?"
            val charAns = buildString {
                if (calloutContent.isNotBlank() && !calloutContent.equals(trimmed, ignoreCase = true)) {
                    append(calloutTitle).append(": ").append(calloutContent).append("\n\n")
                }
                append("Key characteristics emphasized by the teacher:\n• ")
                append(takeaways.take(2).joinToString("\n• "))
            }
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = charQ,
                    answer = charAns,
                    explanation = "Key characteristics and invariants highlighted during the lecture.",
                    status = "Answered from Lecture Notes",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )

            val diffQ = if (isTamil) "இப்பாடத்தின் முக்கிய கருத்துக்களுக்கு இடையேயான வேறுபாடுகள் யாவை?" else "Differentiate between the key concepts discussed in $currentTitle."
            val diffAns = if (defsList.size >= 2) {
                "${defsList[0].first} vs ${defsList[1].first}:\n" +
                "• ${defsList[0].first}: ${defsList[0].second}\n" +
                "• ${defsList[1].first}: ${defsList[1].second}\n" +
                "Each serves distinct functions with different implementation characteristics."
            } else if (typesBullets.size >= 2) {
                "Comparison of key variants:\n• " + typesBullets.take(2).joinToString("\n• ")
            } else {
                "The lecture contrasted standard approaches against optimized representations, emphasizing efficiency trade-offs and structural differences."
            }
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = diffQ,
                    answer = diffAns,
                    explanation = "Comparative breakdown of concepts presented in lecture.",
                    status = "Answered from Lecture",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )

            val exQ = if (isTamil) "$currentTitle பாடத்தின் நடைமுறை உதாரணங்கள் மற்றும் பயன்பாடுகள் யாவை?" else "Give examples and practical applications of $currentTitle."
            val exAns = buildString {
                if (sec3.isNotBlank() && !sec3.equals(trimmed, ignoreCase = true)) append(sec3).append("\n\n")
                if (extractive.detailedNotes.section3Content2.isNotBlank() && extractive.detailedNotes.section3Content2 != sec3 && !extractive.detailedNotes.section3Content2.equals(trimmed, ignoreCase = true)) {
                    append("Practical Context: ").append(extractive.detailedNotes.section3Content2)
                } else {
                    append("These principles are applied in system design to ensure consistency, speed, and reliable data organization.")
                }
            }
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = exQ,
                    answer = exAns,
                    explanation = "Real-world engineering applications and examples.",
                    status = "Answered from Lecture",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )

            val sumQ = if (isTamil) "ஆசிரியர் கற்பித்த பாடத்தின் விரிவான விளக்கம் மற்றும் சுருக்கம் என்ன?" else "Provide a detailed explanation and summary of what the teacher taught."
            val sumAns = buildString {
                if (teacherExp.isNotBlank()) append(teacherExp).append("\n\n")
                append("Summary: ").append(lectureSummary)
            }
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = sumQ,
                    answer = sumAns,
                    explanation = "Comprehensive synthesis of the recorded lecture.",
                    status = "Answered from Lecture",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )

            val diagQ = if (isTamil) "$currentTitle எவ்வாறு கட்டமைக்கப்பட்டுள்ளது? (படிநிலைகள் மற்றும் வரைபடம்)" else "How is $currentTitle structured or represented? Explain the workflow or organization."
            val diagAns = "Structure and Workflow for $currentTitle:\n" +
                "1. Input Data / Source: Captured and categorized according to academic invariants.\n" +
                "2. Core Operations: Processed following the principles taught by the teacher.\n" +
                "3. Outcome / Evaluation: Verified against correctness and structural requirements."
            baselineStudyQAs.add(
                DetectedQuestionAnswerItem(
                    question = diagQ,
                    answer = diagAns,
                    explanation = "Structural workflow and representation of the concept.",
                    status = "Answered from Lecture",
                    isAnswered = true,
                    source = "[From Lecture]"
                )
            )
        }

        val finalBaselineList = baselineStudyQAs.take(10)

        // Populate baseline immediately so UI and tests are responsive
        _uiState.update { state ->
            state.copy(
                teacherExplanation = teacherExp,
                lectureSummary = lectureSummary,
                keyTakeaways = takeaways,
                importantDefinitions = finalDefs,
                studyQuestionsAndAnswers = finalBaselineList,
                detectedQuestionsAndAnswers = finalBaselineList,
                isAnsweringQuestion = false
            )
        }
        SessionManager.setLastGeneratedNote(extractive.generatedNotes.copy(
            aiSummaryText = lectureSummary,
            keyTakeaways = takeaways,
            detectedQuestionsAndAnswers = finalBaselineList
        ))
        SessionManager.setLastDetailedNotes(extractive.detailedNotes)
        SessionManager.setDetectedQuestionsAndAnswers(finalBaselineList)

        return BaselineExtractionResult(
            extractive = extractive,
            resolvedTitle = currentTitle,
            detectedSubject = subjectStr,
            teacherExplanation = teacherExp,
            definitions = finalDefs,
            baselineQuestions = finalBaselineList
        )
    }

    private fun generateSupplementalQuestionsForTopic(
        topic: String,
        subject: String,
        isTamil: Boolean
    ): List<DetectedQuestionAnswerItem> {
        val list = mutableListOf<DetectedQuestionAnswerItem>()
        val isDataStructures = topic.equals("Data Structures", ignoreCase = true) || topic.contains("தரவு அமைப்பு")

        if (isDataStructures) {
            if (isTamil) {
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "தரவு அமைப்பு (Data Structure) என்றால் என்ன? அது கணினி அறிவியலில் ஏன் முக்கியமானது?",
                        answer = "தரவு அமைப்பு என்பது கணினி நினைவகத்தில் தரவை திறமையாக சேமிக்கவும், ஒழுங்கமைக்கவும், கையாளவும் பயன்படும் ஒரு சிறப்பு வடிவமாகும். சரியான தரவு அமைப்பைத் தேர்ந்தெடுப்பது அல்காரிதங்களின் இயங்கு நேரத்தை (Time Complexity) குறைத்து மென்பொருளை வேகமாக இயக்க உதவுகிறது.",
                        explanation = "கணினி அறிவியலின் அடிப்படைக் கருத்து.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "தரவு அமைப்புகளின் முக்கிய வகைகள் மற்றும் பிரிவுகள் யாவை?",
                        answer = "தரவு அமைப்புகள் முக்கியமாக இரு பிரிவுகளாக வகைப்படுத்தப்படுகின்றன:\n• நேரியல் தரவு அமைப்புகள் (Linear Data Structures): உறுப்புகள் தொடர்ச்சியாக வரிசைப்படுத்தப்பட்டிருக்கும் (எ.கா: Arrays, Linked Lists, Stacks, Queues).\n• நேரியல் அல்லாத தரவு அமைப்புகள் (Non-Linear Data Structures): உறுப்புகள் படிநிலையாகவோ அல்லது பல வழிகளில் இணைக்கப்பட்டோ இருக்கும் (எ.கா: Trees, Graphs, Heaps).",
                        explanation = "கட்டமைப்பு வகைப்பாடுகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "தரவு அமைப்புகளின் முக்கிய பண்புகள் மற்றும் மதிப்பீட்டு அளவுகோல்கள் யாவை?",
                        answer = "முக்கிய பண்புகள்:\n1. துல்லியம் (Correctness): தரவு செயல்பாடுகள் சரியான முடிவுகளை தருவதை உறுதி செய்தல்.\n2. நேரத் திறன் (Time Complexity): தேடுதல், சேர்த்தல் மற்றும் நீக்குதல் செயல்பாடுகள் குறைந்த நேரத்தில் இயங்குதல்.\n3. நினைவகத் திறன் (Space Complexity): நினைவக பயன்பாட்டை மிகக் குறைவாகப் பயன்படுத்துதல்.",
                        explanation = "செயல்திறன் பண்புகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "நேரியல் மற்றும் நேரியல் அல்லாத தரவு அமைப்புகளுக்கு இடையேயான வேறுபாடுகள் யாவை?",
                        answer = "முக்கிய வேறுபாடுகள்:\n• அணுகல் முறை: நேரியல் அமைப்பில் உறுப்புகள் ஒற்றை வரிசையில் அணுகப்படும்; நேரியல் அல்லாத அமைப்பில் BFS அல்லது DFS போன்ற சிறப்பு அல்காரிதங்கள் தேவை.\n• நினைவக அமைப்பு: அணிகள் போன்ற நேரியல் அமைப்புகள் தொடர்ச்சியான நினைவகத்தை பயன்படுத்தும்; மரங்கள் மற்றும் வரைபடங்கள் சுட்டிகள் (Pointers) மூலம் இணைக்கப்படும்.\n• உறவுமுறை: நேரியல் என்பது ஒன்றுக்கு-ஒன்று (1-to-1) உறவு; நேரியல் அல்லாதது ஒன்றுக்கு-பல (1-to-Many) அல்லது பல-பல (Many-to-Many) உறவாகும்.",
                        explanation = "ஒப்பீட்டு பகுப்பாய்வு.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "தரவு அமைப்புகளின் நிஜ உலக பயன்பாடுகள் மற்றும் நடைமுறை உதாரணங்கள் யாவை?",
                        answer = "நிஜ உலக மென்பொருள் பயன்பாடுகள்:\n• Stacks: பிரவுசரில் பின்செல்லும் பொத்தான் (Back Button), மற்றும் எடிட்டர்களில் Undo/Redo செயல்பாடுகள்.\n• Queues: ஆப்பரேட்டிங் சிஸ்டத்தில் CPU பணிகள் வரிசைப்படுத்துதல் மற்றும் அச்சுப்பொறி வரிசை (Printer Spooling).\n• Trees: கணினி கோப்பு முறைமை (Folder Structure) மற்றும் தரவுத்தள அட்டவணைக் குறியீடுகள் (B-Trees).\n• Graphs: கூகுள் வரைபட வழிசெலுத்தல் (GPS Navigation) மற்றும் சமூக வலைப்பின்னல் இணைப்புகள்.",
                        explanation = "பொறியியல் பயன்பாடுகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "அடுக்கு (Stack) மற்றும் வரிசை (Queue) ஆகியவற்றின் செயல்பாட்டு முறையை விளக்குக.",
                        answer = "செயல்பாட்டு விதிகள்:\n1. அடுக்கு (Stack - LIFO): கடைசியாக நுழைந்த உறுப்பு முதலில் வெளியேறும். முக்கிய செயல்பாடுகள் push() (சேர்த்தல்), pop() (நீக்குதல்), peek() (பார்த்தல்).\n2. வரிசை (Queue - FIFO): முதலில் நுழைந்த உறுப்பு முதலில் வெளியேறும். முக்கிய செயல்பாடுகள் enqueue() (பின்னே சேர்த்தல்), dequeue() (முன்னிருந்து நீக்குதல்).",
                        explanation = "கட்டமைப்பு இயக்க முறைமை.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
            } else {
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "What is a Data Structure and why is it important in Computer Science?",
                        answer = "A data structure is a specialized format for organizing, processing, retrieving, and storing data efficiently in computer memory. Choosing the right data structure enables algorithms to operate with optimal time and space complexity, ensuring software responsiveness and scalability.",
                        explanation = "Core foundational definition.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Explain the types and classifications of Data Structures.",
                        answer = "Data structures are broadly categorized into two major classes:\n• Linear Data Structures: Elements are arranged sequentially in a single level, where each element has a unique predecessor and successor (e.g., Arrays, Linked Lists, Stacks, Queues).\n• Non-Linear Data Structures: Elements are arranged hierarchically or interconnected across multiple levels without single-sequence order (e.g., Trees, Binary Search Trees, Graphs, Heaps).",
                        explanation = "Classification hierarchy.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "What are the essential characteristics and evaluation criteria of Data Structures?",
                        answer = "Essential characteristics include:\n1. Correctness: Guarantees that data operations strictly preserve integrity and structural invariants.\n2. Time Complexity: Minimizes execution time for critical operations like search, insertion, and deletion under Big-O analysis.\n3. Space Complexity: Optimizes memory layout, avoiding unnecessary pointer overhead and fragmentation.",
                        explanation = "Performance and design criteria.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Differentiate between Linear and Non-Linear Data Structures.",
                        answer = "Key comparisons:\n• Traversal: Linear structures are traversed in a single run (O(N) sequential pass); Non-linear structures require multi-path traversal algorithms like Breadth-First Search (BFS) or Depth-First Search (DFS).\n• Memory Allocation: Linear structures like arrays often occupy contiguous blocks, while non-linear structures use dynamic heap pointers.\n• Relationships: Linear represents 1-to-1 relationships; Non-linear represents 1-to-many (Trees) or many-to-many (Graphs) relationships.",
                        explanation = "Comparative breakdown.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Give practical examples and real-world applications of Data Structures.",
                        answer = "Real-world engineering applications include:\n• Stacks: Web browser back/forward buttons, expression evaluation in compilers, and undo/redo operations in text editors.\n• Queues: Operating system CPU task scheduling, print queues, and asynchronous message brokers (e.g., Kafka, RabbitMQ).\n• Trees: File system folder hierarchies, HTML DOM representations in web browsers, and B-Trees in relational databases.\n• Graphs: Social network recommendation engines, Google Maps shortest-path routing, and network packet routing.",
                        explanation = "Industry use cases.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Explain the core operations and lifecycle of Stacks and Queues.",
                        answer = "Core operations and access rules:\n1. Stack (LIFO - Last In First Out): Operations include push() to insert at top, pop() to remove from top, and peek() to view top element without removal.\n2. Queue (FIFO - First In First Out): Operations include enqueue() to append at rear, dequeue() to remove from front, and front() to inspect the leading element.",
                        explanation = "Operational mechanics.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
            }
        } else {
            // General academic topic
            if (isTamil) {
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic என்றால் என்ன? அதன் முக்கியத்துவம் என்ன?",
                        answer = "$topic என்பது $subject பாடப்பிரிவில் ஒரு முக்கியமான தலைப்பாகும். இது கல்வி மற்றும் நடைமுறை பயன்பாடுகளில் ஆழமாகப் பயன்படுத்தப்படுகிறது.",
                        explanation = "அடிப்படைக் கருத்து விளக்கம்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic பாடத்தின் முக்கிய வகைகள் யாவை?",
                        answer = "$topic பாடப்பிரிவில் பல முக்கிய வகைகள் மற்றும் கோட்பாடுகள் உள்ளன. இவை பல்வேறு பயன்பாடுகளுக்கு ஏற்ப வகைப்படுத்தப்பட்டுள்ளன.",
                        explanation = "வகைகள் மற்றும் பிரிவுகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic பாடத்தின் முக்கிய பண்புகள் யாவை?",
                        answer = "$topic தலைப்பின் முக்கிய பண்புகள், விதிகள் மற்றும் நடைமுறை வழிகாட்டுதல்கள் மாணவர்களின் தேர்வு மற்றும் ஆய்வுத் தேவைகளுக்காக இங்கு விளக்கப்பட்டுள்ளன.",
                        explanation = "முக்கிய பண்புகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic பாடத்தின் நடைமுறை பயன்பாடுகள் யாவை?",
                        answer = "$topic தலைப்பு நவீன தொழில்துறை மற்றும் தொழில்நுட்பத்தில் பல முக்கியமான சிக்கல்களைத் தீர்க்கப் பயன்படுகிறது.",
                        explanation = "நடைமுறை பயன்பாடுகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic தலைப்பில் மாணவர்கள் கவனிக்க வேண்டிய முக்கிய குறிப்புகள் யாவை?",
                        answer = "பாடத்தின் அடிப்படை வரையறைகள், சமன்பாடுகள் அல்லது வரைபடங்கள் மற்றும் மாதிரி வினாக்களை முழுமையாகப் புரிந்துகொள்வது அவசியம்.",
                        explanation = "தேர்வுக்கான முக்கிய குறிப்புகள்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "$topic பாடத்தின் கட்டமைப்பு மற்றும் படிநிலைகள் யாவை?",
                        answer = "$topic பாடப்பிரிவு முறையான கோட்பாடு, செயல்படுத்தல் படிகள் மற்றும் முடிவுகளை சரிபார்க்கும் செயல்முறையைக் கொண்டுள்ளது.",
                        explanation = "படிநிலை விளக்கம்.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
            } else {
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "What is $topic and why is it important in $subject?",
                        answer = "$topic is an essential academic subject in $subject, establishing the theoretical foundations and practical principles required for mastery of this discipline.",
                        explanation = "Core foundational concept.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Explain the types and classifications of $topic.",
                        answer = "The primary classifications of $topic encompass its fundamental theoretical variants, optimized representations, and context-specific implementation models.",
                        explanation = "Structural categories.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "What are the essential characteristics and properties of $topic?",
                        answer = "Core characteristics include structural correctness, adherence to domain invariants, consistent evaluation rules, and operational stability across varied input scenarios.",
                        explanation = "Domain invariants and properties.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Differentiate between the key approaches in $topic.",
                        answer = "Different approaches in $topic contrast classical baseline models against optimized, modern frameworks, balancing complexity, reliability, and computational efficiency.",
                        explanation = "Comparative breakdown.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Give practical examples and real-world applications of $topic.",
                        answer = "$topic is widely applied across modern software engineering, scientific research, and real-world system design to solve complex domain challenges reliably.",
                        explanation = "Practical industry applications.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
                list.add(
                    DetectedQuestionAnswerItem(
                        question = "Explain the structural representation and workflow of $topic.",
                        answer = "The operational workflow for $topic consists of: 1) Input initialization and validation; 2) Deterministic rule-based processing; 3) Verification of correctness and final state evaluation.",
                        explanation = "Step-by-step structural workflow.",
                        status = "AI Explanation",
                        isAnswered = true,
                        source = "[Supplemental AI Explanation]"
                    )
                )
            }
        }
        return list
    }

    /**
     * Comprehensive Phase 6A Lecture Understanding Engine:
     * - Understands and explains what the teacher is teaching via Google Gemini & Sherpa-ONNX.
     * - Extracts important topics, definitions and key points.
     * - Generates 5 to 10 useful study questions and accurate answers based strictly on the recorded lecture.
     * - Generates lecture summary and revision questions.
     * - Caches analysis by transcript content to avoid repeated Gemini requests.
     * - Supports English and Tamil.
     * - Displays loading, retry, and error states gracefully.
     */
    fun extractLectureIntelligence(
        text: String,
        paragraphs: List<TranscriptParagraph> = emptyList(),
        forceRefresh: Boolean = false
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onClearTextClicked()
            return
        }

        val pList = if (paragraphs.isNotEmpty()) paragraphs else buildParagraphsFromText(trimmed)
        val baseline = synthesizeExtractiveBaseline(trimmed, pList)
        textDebounceJob?.cancel()
        invokeRemoteGeminiAnalysis(trimmed, pList, forceRefresh = forceRefresh, baseline = baseline)
    }

    private fun invokeRemoteGeminiAnalysis(
        trimmed: String,
        paragraphs: List<TranscriptParagraph>,
        forceRefresh: Boolean = false,
        baseline: BaselineExtractionResult? = null
    ) {
        val currentLang = _uiState.value.selectedLanguageCode

        // Prevent duplicate concurrent analysis requests for identical text and language
        if (isAnalysisInProgress && trimmed == inProgressText && currentLang == inProgressLanguage) {
            return
        }

        // Avoid repeated Gemini calls if already analyzed with 5+ questions
        if (!forceRefresh &&
            trimmed == lastAnalyzedText &&
            currentLang == lastAnalyzedLanguage &&
            _uiState.value.studyQuestionsAndAnswers.size >= 5 &&
            _uiState.value.teacherExplanation.isNotBlank()
        ) {
            return
        }

        intelligenceJob?.cancel()
        questionAnsweringJob?.cancel()

        intelligenceJob = viewModelScope.launch(Dispatchers.Default) {
            isAnalysisInProgress = true
            inProgressText = trimmed
            inProgressLanguage = currentLang
            try {
                _uiState.update {
                    it.copy(
                        isAnalyzingIntelligence = true,
                        isAnsweringQuestion = true,
                        canRetryAnalysis = false,
                        analysisErrorMessage = null
                    )
                }

                val pList = if (paragraphs.isNotEmpty()) paragraphs else buildParagraphsFromText(trimmed)
                val activeBaseline = baseline ?: synthesizeExtractiveBaseline(trimmed, pList)
                val currentTitle = activeBaseline.resolvedTitle.ifBlank { _uiState.value.lectureTitle.ifBlank { "Lecture Notes" } }
                val subjectStr = activeBaseline.detectedSubject.ifBlank { _uiState.value.selectedSubject.substringBefore(" -").trim() }
                val durationStr = _uiState.value.totalTimeText
                val isTamil = currentLang == "ta"

                val finalBaselineList = activeBaseline.baselineQuestions
                val extractive = activeBaseline.extractive
                val teacherExp = activeBaseline.teacherExplanation
                val defsList = activeBaseline.definitions

                // Step 2: Now invoke Gemini via ChatRepository for advanced AI intelligence
                val isUserLoggedIn = SessionManager.isLoggedIn()
                if (!isUserLoggedIn) {
                    _uiState.update {
                        it.copy(
                            isAnalyzingIntelligence = false,
                            isAnsweringQuestion = false
                        )
                    }
                    lastAnalyzedText = trimmed
                    lastAnalyzedLanguage = currentLang
                    return@launch
                }

            val langPromptName = if (isTamil) "Tamil (தமிழ்)" else "English"
            val prompt = """
            You are SmartNotes AI, an expert academic professor and tutor. Analyze the following recorded lecture transcript and generate complete study intelligence with comprehensive questions and detailed answers.
            
            Language: $langPromptName.
            Detected Lecture Topic / Title: $currentTitle
            Academic Subject: $subjectStr
            
            Recorded Lecture Transcript:
            $trimmed
            
            MANDATORY RULES FOR TOPIC FIDELITY & SHORT TRANSCRIPTS:
            1. TOPIC FIDELITY: Your entire response MUST focus strictly on the detected lecture topic: "$currentTitle" and subject: "$subjectStr".
               - NEVER generate questions or notes about unrelated subjects. If the topic is "$currentTitle", do NOT mention "Database Normalization" or unrelated database concepts unless the transcript specifically teaches that.
            2. SHORT / INTRODUCTORY TRANSCRIPTS:
               - If the transcript is brief or introductory (e.g. introducing the topic of "$currentTitle"), acknowledge that the recorded lecture is an introductory overview.
               - In that case, generate Question 1 strictly grounded in what the teacher announced with source "[From Lecture]".
               - For questions 2 through 7 (5 to 10 total), provide comprehensive, high-quality educational study questions and answers covering "$currentTitle" (definition, classifications/types, characteristics, comparisons, practical examples, architecture/workflow).
               - Strictly tag these supplemental educational questions with source "[Supplemental AI Explanation]". NEVER label supplemental information as "[From Lecture]".
            3. EVERY question MUST have its own COMPLETE, ACCURATE, DETAILED, student-friendly answer:
               - Explain the concept thoroughly with definitions, examples, steps, or differences where relevant.
               - Never provide a one-word, superficial, or incomplete answer.
               - Never repeat raw transcript text verbatim as an answer.
            4. STRICT NEGATIVE RULES:
               - NEVER generate meaningless questions from conversational filler words or student greetings (e.g. NEVER generate "What is Students?", "What is Today?", "What is Class?", "What is Morning?", "What is Today we are...").
               - NEVER produce duplicate or near-duplicate questions.
            5. Also provide:
               - teacher_explanation: A clear, educational explanation of what the teacher taught and introduced.
               - summary: A readable lecture summary.
               - key_takeaways: 3 to 5 core takeaways.
               - definitions: Array of key academic terms and their definitions.

            Respond ONLY with a valid JSON object matching this exact schema:
            {
              "teacher_explanation": "...",
              "summary": "...",
              "key_takeaways": ["...", "..."],
              "definitions": [
                {"term": "...", "definition": "..."}
              ],
              "study_questions": [
                {
                  "question": "...",
                  "answer": "...",
                  "explanation": "...",
                  "source": "[From Lecture] or [Supplemental AI Explanation]"
                }
              ]
            }
            """.trimIndent()

            val chatResult = ChatRepository.sendMessage(
                message = prompt,
                conversationId = null,
                subject = subjectStr,
                lectureTitle = currentTitle
            )

            if (chatResult is ChatResult.Success) {
                val assistantContent = chatResult.data.assistantMessage?.content ?: ""
                val parsed = parseGeminiAnalysisJson(assistantContent)

                if (parsed != null && (parsed.teacherExplanation.isNotBlank() || parsed.studyQuestions.isNotEmpty())) {
                    val isDbTopic = com.example.Simtas.data.ml.LectureNotesGenerator.isTopicAboutDatabaseNormalization(trimmed)
                    val finalQuestions = mutableListOf<DetectedQuestionAnswerItem>()
                    for (qItem in parsed.studyQuestions) {
                        val lowerQ = qItem.question.lowercase(Locale.ROOT)
                        val isUnrelatedDb = !isDbTopic && (lowerQ.contains("normalization") || lowerQ.contains("1nf") || lowerQ.contains("2nf") || lowerQ.contains("3nf"))
                        val isFiller = lowerQ.contains("today we are") || lowerQ.contains("good morning") || lowerQ.contains("what is students")
                        if (!isUnrelatedDb && !isFiller && finalQuestions.none { it.question.equals(qItem.question, ignoreCase = true) }) {
                            finalQuestions.add(qItem)
                        }
                    }

                    // Ensure between 5 and 10 questions by supplementing from baseline if needed
                    if (finalQuestions.size < 5) {
                        for (fallbackItem in finalBaselineList) {
                            if (finalQuestions.size >= 5) break
                            if (finalQuestions.none { it.question.equals(fallbackItem.question, ignoreCase = true) }) {
                                finalQuestions.add(fallbackItem)
                            }
                        }
                    }

                    val finalTakeaways = if (parsed.keyTakeaways.isNotEmpty()) parsed.keyTakeaways else extractive.generatedNotes.keyTakeaways
                    val finalDefs = if (parsed.definitions.isNotEmpty()) parsed.definitions else defsList
                    val finalSummary = if (parsed.summary.isNotBlank()) parsed.summary else extractive.generatedNotes.aiSummaryText
                    val finalExplanation = if (parsed.teacherExplanation.isNotBlank()) parsed.teacherExplanation else teacherExp

                    val updatedGenNotes = extractive.generatedNotes.copy(
                        aiSummaryText = finalSummary,
                        keyTakeaways = finalTakeaways,
                        detectedQuestionsAndAnswers = finalQuestions.take(10)
                    )

                    SessionManager.setLastGeneratedNote(updatedGenNotes)
                    SessionManager.setDetectedQuestionsAndAnswers(finalQuestions.take(10))

                    // Persist to LectureNotesStorage
                    val audioId = SessionManager.getLastUploadedAudioId()
                    val lectureId = audioId?.toString() ?: (System.currentTimeMillis() / 1000).toString()
                    LectureNotesStorage.saveLecture(
                        lectureId = lectureId,
                        title = currentTitle,
                        subject = subjectStr,
                        transcriptText = trimmed,
                        generatedNotes = updatedGenNotes,
                        detailedNotes = extractive.detailedNotes,
                        durationText = durationStr
                    )

                    lastAnalyzedText = trimmed
                    lastAnalyzedLanguage = currentLang

                    _uiState.update { state ->
                        state.copy(
                            teacherExplanation = finalExplanation,
                            lectureSummary = finalSummary,
                            keyTakeaways = finalTakeaways,
                            importantDefinitions = finalDefs.take(5),
                            studyQuestionsAndAnswers = finalQuestions.take(10),
                            detectedQuestionsAndAnswers = finalQuestions.take(10),
                            isAnalyzingIntelligence = false,
                            isAnsweringQuestion = false,
                            canRetryAnalysis = false,
                            analysisErrorMessage = null
                        )
                    }

                    // Background sync transcription if audioId is valid
                    if (audioId != null && audioId > 0) {
                        try {
                            AudioRepository.saveTranscription(
                                audioId = audioId,
                                lectureTitle = currentTitle,
                                fullTranscript = trimmed,
                                paragraphs = pList,
                                duration = durationStr,
                                language = if (isTamil) "Tamil" else "English"
                            )
                        } catch (_: Exception) {}
                    }
                    return@launch
                }
            }

            // If Gemini call failed: keep offline baseline and show retry option
            _uiState.update { state ->
                state.copy(
                    isAnalyzingIntelligence = false,
                    isAnsweringQuestion = false,
                    canRetryAnalysis = true,
                    analysisErrorMessage = if (chatResult is ChatResult.Error) {
                        "Cloud AI analysis: ${chatResult.message}. Offline lecture notes ready."
                    } else {
                        "AI analysis formatted with offline engine."
                    }
                )
            }
            lastAnalyzedText = trimmed
            lastAnalyzedLanguage = currentLang
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isAnalyzingIntelligence = false,
                        isAnsweringQuestion = false,
                        canRetryAnalysis = true,
                        analysisErrorMessage = "Cloud analysis interrupted: ${e.localizedMessage ?: "Network error"}. Local notes ready."
                    )
                }
            } finally {
                isAnalysisInProgress = false
                inProgressText = null
                inProgressLanguage = null
                _uiState.update { state ->
                    state.copy(
                        isAnalyzingIntelligence = false,
                        isAnsweringQuestion = false
                    )
                }
            }
        }
    }

    private data class ParsedLectureAnalysis(
        val teacherExplanation: String,
        val summary: String,
        val keyTakeaways: List<String>,
        val definitions: List<Pair<String, String>>,
        val studyQuestions: List<DetectedQuestionAnswerItem>
    )

    private fun parseGeminiAnalysisJson(rawResponse: String): ParsedLectureAnalysis? {
        if (rawResponse.isBlank()) return null

        var cleanJson = rawResponse.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json")
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```")
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```")
        }
        cleanJson = cleanJson.trim()

        val jsonStart = cleanJson.indexOf('{')
        val jsonEnd = cleanJson.lastIndexOf('}')
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleanJson = cleanJson.substring(jsonStart, jsonEnd + 1)
        }

        // Attempt 1: Full standard JSONObject parse
        try {
            val obj = JSONObject(cleanJson)
            val explanation = obj.optString("teacher_explanation", "").trim()
            val summary = obj.optString("summary", "").trim()

            val takeaways = mutableListOf<String>()
            val takeawaysArr = obj.optJSONArray("key_takeaways")
            if (takeawaysArr != null) {
                for (i in 0 until takeawaysArr.length()) {
                    val t = takeawaysArr.optString(i, "").trim()
                    if (t.isNotBlank()) takeaways.add(t)
                }
            }

            val definitions = mutableListOf<Pair<String, String>>()
            val defsArr = obj.optJSONArray("definitions")
            if (defsArr != null) {
                for (i in 0 until defsArr.length()) {
                    val dObj = defsArr.optJSONObject(i)
                    if (dObj != null) {
                        val term = dObj.optString("term", "").trim()
                        val def = dObj.optString("definition", "").trim()
                        if (term.isNotBlank() && def.isNotBlank()) {
                            definitions.add(term to def)
                        }
                    }
                }
            }

            val questions = mutableListOf<DetectedQuestionAnswerItem>()
            val qArr = obj.optJSONArray("study_questions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    val qObj = qArr.optJSONObject(i)
                    if (qObj != null) {
                        val item = parseSingleQuestionObject(qObj)
                        if (item != null && questions.none { it.question.equals(item.question, ignoreCase = true) }) {
                            questions.add(item)
                        }
                    }
                }
            }

            if (explanation.isNotBlank() || questions.isNotEmpty() || summary.isNotBlank()) {
                return ParsedLectureAnalysis(
                    teacherExplanation = explanation,
                    summary = summary,
                    keyTakeaways = takeaways,
                    definitions = definitions,
                    studyQuestions = questions
                )
            }
        } catch (_: Exception) {
            // Full JSON parse failed (e.g. truncated response). Proceed to safe recovery parser.
        }

        // Attempt 2: Safe recovery parser - NEVER blindly append closing braces to incomplete strings.
        // Instead, safely recover completed fields and completed question-answer objects.
        return recoverCompletedLectureAnalysis(rawResponse)
    }

    private fun parseSingleQuestionObject(qObj: JSONObject): DetectedQuestionAnswerItem? {
        val q = qObj.optString("question", "").trim()
        val a = qObj.optString("answer", "").trim()
        val exp = qObj.optString("explanation", "").trim()
        val src = qObj.optString("source", "").trim()
        val finalSource = if (src.isNotBlank()) {
            if (!src.startsWith("[")) "[$src]" else src
        } else {
            "[From Lecture]"
        }

        val lowerQ = q.lowercase(Locale.ROOT)
        val isFiller = CONVERSATIONAL_EXCLUSIONS.any { ex ->
            lowerQ.contains("what is $ex") || lowerQ.contains("what are $ex")
        } || com.example.Simtas.data.ml.LectureNotesGenerator.isConversationalFiller(lowerQ)
          || lowerQ.contains("today we are")
          || lowerQ.contains("good morning")
          || lowerQ.contains("what is students")

        val isCompleteAnswer = a.length >= 15 && !isFiller

        return if (q.isNotBlank() && isCompleteAnswer) {
            DetectedQuestionAnswerItem(
                question = q,
                answer = a,
                explanation = if (exp.isNotBlank()) exp else "Generated from lecture content.",
                status = if (finalSource.contains("Supplemental", ignoreCase = true)) "Supplemental AI Explanation" else "Answered from Lecture",
                isAnswered = true,
                source = finalSource
            )
        } else {
            null
        }
    }

    private fun recoverCompletedLectureAnalysis(rawResponse: String): ParsedLectureAnalysis? {
        val explanation = extractJsonStringField(rawResponse, "teacher_explanation")
        val summary = extractJsonStringField(rawResponse, "summary")
        val takeaways = extractJsonArrayStrings(rawResponse, "key_takeaways")
        val definitions = extractCompletedDefinitions(rawResponse)
        val questions = extractCompletedStudyQuestions(rawResponse)

        if (explanation.isBlank() && summary.isBlank() && questions.isEmpty()) {
            return null
        }

        return ParsedLectureAnalysis(
            teacherExplanation = explanation,
            summary = summary,
            keyTakeaways = takeaways,
            definitions = definitions,
            studyQuestions = questions
        )
    }

    private fun extractJsonStringField(raw: String, fieldName: String): String {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = pattern.find(raw) ?: return ""
        val escapedValue = match.groupValues[1]
        return unescapeJsonString(escapedValue).trim()
    }

    private fun extractJsonArrayStrings(raw: String, arrayName: String): List<String> {
        val startIdx = raw.indexOf("\"$arrayName\"")
        if (startIdx == -1) return emptyList()
        val bracketIdx = raw.indexOf('[', startIdx)
        if (bracketIdx == -1) return emptyList()

        val results = mutableListOf<String>()
        val strPattern = Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
        val sub = raw.substring(bracketIdx)
        val closeBracket = sub.indexOf(']')
        val arrayContent = if (closeBracket != -1) sub.substring(0, closeBracket + 1) else sub

        for (match in strPattern.findAll(arrayContent)) {
            val unescaped = unescapeJsonString(match.groupValues[1]).trim()
            if (unescaped.isNotBlank() && unescaped != arrayName) {
                results.add(unescaped)
            }
        }
        return results
    }

    private fun extractCompletedDefinitions(raw: String): List<Pair<String, String>> {
        val defsStart = raw.indexOf("\"definitions\"")
        if (defsStart == -1) return emptyList()
        val sub = raw.substring(defsStart)
        val results = mutableListOf<Pair<String, String>>()

        for (block in extractBalancedBracedBlocks(sub)) {
            try {
                val obj = JSONObject(block)
                val term = obj.optString("term", "").trim()
                val def = obj.optString("definition", "").trim()
                if (term.isNotBlank() && def.isNotBlank() && results.none { it.first.equals(term, ignoreCase = true) }) {
                    results.add(term to def)
                }
            } catch (_: Exception) {}
        }
        return results
    }

    private fun extractCompletedStudyQuestions(raw: String): List<DetectedQuestionAnswerItem> {
        val qStart = raw.indexOf("\"study_questions\"")
        if (qStart == -1) return emptyList()
        val sub = raw.substring(qStart)
        val questions = mutableListOf<DetectedQuestionAnswerItem>()

        for (block in extractBalancedBracedBlocks(sub)) {
            try {
                val qObj = JSONObject(block)
                val item = parseSingleQuestionObject(qObj)
                if (item != null) {
                    val a = item.answer.trim()
                    val hasCompletePunctuation = a.endsWith(".") || a.endsWith("!") || a.endsWith("?") || a.endsWith(":") || a.endsWith("```") || a.endsWith(")") || a.endsWith("\"")
                    if (hasCompletePunctuation && questions.none { it.question.equals(item.question, ignoreCase = true) }) {
                        questions.add(item)
                    }
                }
            } catch (_: Exception) {}
        }
        return questions
    }

    private fun extractBalancedBracedBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        var depth = 0
        var blockStart = -1
        var inString = false
        var escape = false

        for (i in text.indices) {
            val c = text[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        blockStart = i
                    }
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && blockStart != -1) {
                        blocks.add(text.substring(blockStart, i + 1))
                        blockStart = -1
                    }
                }
            }
        }
        return blocks
    }

    private fun unescapeJsonString(str: String): String {
        return str
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    // --- Interactive Follow-Up Questions (Screen 2) ---

    fun onFollowUpQuestionChanged(text: String) {
        _uiState.update { it.copy(followUpQuestionText = text) }
    }

    fun askFollowUpQuestion(explicitQuestion: String? = null) {
        val q = (explicitQuestion ?: _uiState.value.followUpQuestionText).trim()
        if (q.isBlank()) return

        val transcript = _uiState.value.recognizedText.trim()
        val detNotes = SessionManager.getLastDetailedNotes()
        val genNote = SessionManager.getLastGeneratedNote()

        // 1. Immediately provide offline answer for instant response & unit test compatibility
        val offlineAnswer = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(
            question = q,
            transcriptText = transcript,
            detailedNotes = detNotes,
            summaryText = genNote?.aiSummaryText ?: _uiState.value.lectureSummary,
            keyTakeaways = genNote?.keyTakeaways ?: _uiState.value.keyTakeaways
        )

        _uiState.update { state ->
            state.copy(
                followUpQaList = state.followUpQaList + offlineAnswer,
                followUpQuestionText = "",
                isAskingFollowUp = false
            )
        }

        // 2. If logged in and transcript exists, query Gemini in background to enrich answer
        if (SessionManager.isLoggedIn() && transcript.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val isTamil = _uiState.value.selectedLanguageCode == "ta"
                    val langName = if (isTamil) "Tamil (தமிழ்)" else "English"
                    val currentTitle = _uiState.value.lectureTitle.ifBlank { "Recorded Lecture" }

                    val prompt = """
                    The student is asking a follow-up question about the lecture '$currentTitle'.
                    
                    Lecture Transcript:
                    $transcript
                    
                    Student Question:
                    $q
                    
                    Instructions:
                    1. Answer clearly in $langName.
                    2. If the answer is covered in the teacher's lecture, start your answer with "[From Lecture] ".
                    3. If the question asks for information or explanation beyond what was covered in the lecture, start your answer with "[Supplemental AI Explanation] ".
                    """.trimIndent()

                    val chatResult = ChatRepository.sendMessage(
                        message = prompt,
                        conversationId = null,
                        subject = _uiState.value.selectedSubject,
                        lectureTitle = currentTitle
                    )

                    if (chatResult is ChatResult.Success) {
                        val rawContent = chatResult.data.assistantMessage?.content?.trim() ?: ""
                        if (rawContent.isNotBlank()) {
                            val isFromLecture = rawContent.startsWith("[From Lecture]")
                            val cleanAnswer = rawContent
                                .removePrefix("[From Lecture]")
                                .removePrefix("[Supplemental AI Explanation]")
                                .trim()

                            val enrichedItem = DetectedQuestionAnswerItem(
                                question = q,
                                answer = cleanAnswer,
                                explanation = if (isFromLecture) "Answered from lecture recording." else "Supplemental AI explanation.",
                                status = if (isFromLecture) "Answered from Lecture" else "Supplemental AI Explanation",
                                isAnswered = true,
                                source = if (isFromLecture) "[From Lecture]" else "[Supplemental AI Explanation]"
                            )

                            _uiState.update { state ->
                                val updatedList = state.followUpQaList.toMutableList()
                                val lastIndex = updatedList.indexOfLast { it.question == q }
                                if (lastIndex != -1) {
                                    updatedList[lastIndex] = enrichedItem
                                } else {
                                    updatedList.add(enrichedItem)
                                }
                                state.copy(followUpQaList = updatedList)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun retryLectureAnalysis(context: Context? = null) {
        _uiState.update {
            it.copy(
                analysisErrorMessage = null,
                canRetryAnalysis = false
            )
        }
        val text = _uiState.value.recognizedText
        if (text.isNotBlank()) {
            val paragraphs = buildParagraphsFromText(text)
            extractLectureIntelligence(text, paragraphs, forceRefresh = true)
        }
    }

    fun retryTranscription(context: Context) {
        _uiState.update {
            it.copy(
                errorMessage = null,
                transcriptionFailed = false,
                canRetryTranscription = false
            )
        }
        transcribeLoadedAudio(context)
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
