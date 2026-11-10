package com.example.Simtas.ui.preview

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem

/**
 * UI State for Lecture Preview Screen following MVVM architecture.
 * Holds audio playback state, resolved file metadata, lecture details,
 * real-time voice-to-text / manual text editing states, and Phase 6A
 * lecture intelligence: Teacher's explanation, Key Takeaways, Definitions,
 * Summary, Automatically generated Study Questions, and Follow-Up Q&A.
 */
data class LecturePreviewUiState(
    val subjectBadge: String = "Computer Science",
    val lectureTitle: String = "Database Normalization",
    val dateDurationCodeText: String = "Recorded · CS301",
    val statusText: String = "Ready to transcribe",
    val currentTimeText: String = "00:00",
    val totalTimeText: String = "00:00",
    val isPlaying: Boolean = false,
    val playbackSpeedText: String = "1x",
    val playbackProgress: Float = 0f,
    val selectedSubject: String = "Computer Science - CS301",
    val subjectOptions: List<String> = listOf("Computer Science - CS301", "Mathematics - MATH201", "Physics - PHYS101"),
    val fileMetadataText: String = "No audio selected",
    val audioFilePath: String? = null,
    val audioFileName: String = "",
    val fileSizeBytes: Long = 0L,
    val hasAudioFile: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    // Real-time Voice-to-Text & Manual Input additions
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val isTranscribing: Boolean = false,
    val transcriptionProgress: Int = 0,
    val selectedLanguageCode: String = "en",
    val supportedLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ta" to "Tamil (தமிழ்)",
        "" to "Auto Detect"
    ),
    val isSaved: Boolean = false,
    // Automatic Question Answering additions
    val detectedQuestionsAndAnswers: List<DetectedQuestionAnswerItem> = emptyList(),
    val isAnsweringQuestion: Boolean = false,

    // Phase 6A: Lecture Understanding & Intelligence additions
    val teacherExplanation: String = "",
    val lectureSummary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val importantDefinitions: List<Pair<String, String>> = emptyList(),
    val studyQuestionsAndAnswers: List<DetectedQuestionAnswerItem> = emptyList(),
    val followUpQuestionText: String = "",
    val followUpQaList: List<DetectedQuestionAnswerItem> = emptyList(),
    val isAnalyzingIntelligence: Boolean = false,
    val transcriptionFailed: Boolean = false,
    val canRetryTranscription: Boolean = false
)
