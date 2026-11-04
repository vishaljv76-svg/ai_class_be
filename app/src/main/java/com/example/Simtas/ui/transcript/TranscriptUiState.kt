package com.example.Simtas.ui.transcript

import com.example.Simtas.data.ml.OfflineSpeechRecognizer

data class TranscriptParagraph(
    val timestamp: String,
    val speaker: String,
    val text: String,
    val isHighlighted: Boolean = false
)

/**
 * UI State for Transcript Screen following MVVM architecture.
 * Supports offline multilingual transcription (English, Tamil, and 90+ languages),
 * model download tracking, progress indication, and editable transcript content.
 */
data class TranscriptUiState(
    val lectureTitle: String = "Database Normalization",
    val language: String = "Auto Detect",
    val selectedLanguageCode: String = "",
    val supportedLanguages: List<Pair<String, String>> = OfflineSpeechRecognizer.SUPPORTED_LANGUAGES,
    val durationAndConfidence: String = "",
    val searchQuery: String = "",
    val currentTimeText: String = "00:00",
    val totalTimeText: String = "00:00",
    val isPlaying: Boolean = false,
    val paragraphs: List<TranscriptParagraph> = emptyList(),
    val excerptFooterText: String = "",
    val isLoading: Boolean = false,
    val isModelInstalled: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val modelDownloadProgress: Int = 0,
    val modelDownloadStatusText: String = "",
    val isTranscribing: Boolean = false,
    val transcriptionProgress: Int = 0,
    val transcriptionStatusText: String = "",
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editableText: String = "",
    val isSaved: Boolean = false,
    val hasAudioFile: Boolean = false,
    val audioFilePath: String? = null
)
