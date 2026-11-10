package com.example.Simtas.ui.record

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

/**
 * UI State for Record Lecture Screen following MVVM architecture.
 * Supports smooth start/pause/resume/stop, dynamic amplitude waveform,
 * multilingual (English/Tamil) selection, and post-recording pathways.
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
    val uploadedAudioId: Int? = null,
    val selectedLanguageCode: String = "en",
    val audioAmplitudes: List<Float> = List(20) { 0.15f },
    val audioLevelNormalized: Float = 0f,
    val audioLevelCategory: String = "Ready",
    val isPermissionDenied: Boolean = false
)
