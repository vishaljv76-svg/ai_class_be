package com.example.Simtas.ui.record

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

/**
 * UI State for Record Lecture Screen following MVVM architecture.
 */
data class RecordLectureUiState(
    val courseCode: String = "Computer Science - CS301",
    val lectureTitle: String = "Database Normalization",
    val professorAndDate: String = "Prof. Sarah Chen · 21 Oct 2026",
    val timerText: String = "00:00",
    val elapsedSeconds: Long = 0L,
    val status: RecordingStatus = RecordingStatus.IDLE,
    val statusText: String = "Tap microphone or Start to begin",
    val micConnectedText: String = "Microphone ready · Tap to record",
    val tipText: String = "Stay present. Your recording is saved locally while you capture the lecture.",
    val recordedFilePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val formattedFileSize: String = "",
    val errorMessage: String? = null,
    val isUploading: Boolean = false,
    val uploadedAudioId: Int? = null
)
