package com.example.Simtas.ui.preview

/**
 * UI State for Lecture Preview Screen following MVVM architecture.
 * Holds audio playback state, resolved file metadata, and lecture details.
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
    val isLoading: Boolean = false
)
