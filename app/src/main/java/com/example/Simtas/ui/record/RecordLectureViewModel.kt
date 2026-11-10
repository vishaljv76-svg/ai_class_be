package com.example.Simtas.ui.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AudioRepository
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
import kotlin.math.sin

/**
 * ViewModel for [RecordLectureScreen] adhering to MVVM architecture.
 * Manages audio recording lifecycle with MediaRecorder, permission validation,
 * dynamic live amplitude waveform monitoring, multilingual (English & Tamil)
 * audio capture, safe interruption recovery, and session persistence.
 */
class RecordLectureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RecordLectureUiState())
    val uiState: StateFlow<RecordLectureUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var elapsedSeconds: Long = 0L
    private val recorderLock = Any()

    @Volatile
    private var isRecordingSessionActive: Boolean = false

    init {
        // Initialize language from SessionManager if previously selected
        val sessionLang = SessionManager.getSelectedLanguage()
        if (sessionLang.isNotBlank()) {
            _uiState.update { it.copy(selectedLanguageCode = sessionLang) }
        }
    }

    /**
     * Updates lecture title for customized notes and file naming.
     */
    fun updateLectureTitle(title: String) {
        _uiState.update { it.copy(lectureTitle = title.trim()) }
    }

    /**
     * Updates course code for customized notes and file naming.
     */
    fun updateCourseCode(code: String) {
        _uiState.update { it.copy(courseCode = code.trim()) }
    }

    /**
     * Sets preferred recording language ("en" for English, "ta" for Tamil).
     */
    fun setLanguage(languageCode: String) {
        val code = if (languageCode == "ta") "ta" else "en"
        SessionManager.setSelectedLanguage(code)
        _uiState.update { it.copy(selectedLanguageCode = code) }
    }

    /**
     * Starts audio recording with safe directory creation, unique file naming,
     * permission validation, and real-time audio amplitude metering.
     */
    fun startRecording(context: Context) {
        synchronized(recorderLock) {
            if (isRecordingSessionActive || mediaRecorder != null) {
                // Prevent multiple simultaneous recording sessions
                return
            }

            // Verify permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                onPermissionDenied()
                return
            }

            // Clear previous lecture artifacts for session isolation
            SessionManager.clearSessionLectureData()

            try {
                // 1. Prepare private storage directory for audio recordings
                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs()
                }

                // 2. Generate meaningful filename with course, title, language, and timestamp
                val cleanSubject = _uiState.value.courseCode
                    .replace(Regex("[^a-zA-Z0-9]"), "_")
                    .trim('_')
                    .take(12)
                    .ifBlank { "Lecture" }

                val cleanTitle = _uiState.value.lectureTitle
                    .replace(Regex("[^a-zA-Z0-9]"), "_")
                    .trim('_')
                    .take(24)
                    .ifBlank { "Audio" }

                val langSuffix = if (_uiState.value.selectedLanguageCode == "ta") "_ta" else "_en"
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(recordingsDir, "lecture_${cleanSubject}_${cleanTitle}${langSuffix}_${timestamp}.m4a")
                currentOutputFile = outputFile

                // 3. Initialize MediaRecorder API
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

                mediaRecorder = recorder
                isRecordingSessionActive = true
                elapsedSeconds = 0L

                _uiState.update {
                    it.copy(
                        status = RecordingStatus.RECORDING,
                        statusText = "Recording in progress",
                        micConnectedText = "Capturing clearly · Microphone connected",
                        timerText = "00:00",
                        elapsedSeconds = 0L,
                        errorMessage = null,
                        isPermissionDenied = false,
                        recordedFilePath = null,
                        fileSizeBytes = 0L,
                        formattedFileSize = "",
                        audioLevelCategory = "Listening..."
                    )
                }

                startTimer()
                startAmplitudeTracking()

            } catch (e: Exception) {
                releaseRecorderInternal()
                _uiState.update {
                    it.copy(
                        status = RecordingStatus.IDLE,
                        statusText = "Recording failed to start",
                        errorMessage = "Microphone error: ${e.localizedMessage ?: "Unable to access microphone"}"
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_uiState.value.status == RecordingStatus.RECORDING) {
                    elapsedSeconds++
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    val timeStr = String.format(Locale.US, "%02d:%02d", mins, secs)
                    _uiState.update {
                        it.copy(timerText = timeStr, elapsedSeconds = elapsedSeconds)
                    }
                }
            }
        }
    }

    /**
     * Polls actual microphone amplitude levels for dynamic live audio waveform visualization.
     */
    private fun startAmplitudeTracking() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(80L)
                if (_uiState.value.status == RecordingStatus.RECORDING) {
                    val rawAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }

                    // Normalize between 0.08f (subtle baseline) and 1.0f (peak)
                    val normalized = if (rawAmp > 0) {
                        (rawAmp / 28000f).coerceIn(0.08f, 1.0f)
                    } else {
                        // Gentle natural idle wave when quiet
                        val wave = 0.08f + (sin(System.currentTimeMillis() / 250.0).toFloat() * 0.04f).coerceAtLeast(0f)
                        wave.coerceIn(0.08f, 0.20f)
                    }

                    val currentList = _uiState.value.audioAmplitudes
                    val updatedList = if (currentList.size >= 20) {
                        currentList.drop(1) + normalized
                    } else {
                        currentList + normalized
                    }

                    val category = when {
                        normalized > 0.65f -> "Strong Signal"
                        normalized > 0.25f -> "Good Signal"
                        else -> "Capturing..."
                    }

                    _uiState.update {
                        it.copy(
                            audioAmplitudes = updatedList,
                            audioLevelNormalized = normalized,
                            audioLevelCategory = category
                        )
                    }
                }
            }
        }
    }

    fun onPauseClicked() {
        synchronized(recorderLock) {
            val recorder = mediaRecorder
            if (recorder != null && _uiState.value.status == RecordingStatus.RECORDING) {
                try {
                    recorder.pause()
                    amplitudeJob?.cancel()
                    _uiState.update {
                        it.copy(
                            status = RecordingStatus.PAUSED,
                            statusText = "Recording paused",
                            micConnectedText = "Microphone paused · Tap Resume to continue",
                            audioLevelCategory = "Paused"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Pause failed: ${e.localizedMessage ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    fun onResumeClicked() {
        synchronized(recorderLock) {
            val recorder = mediaRecorder
            if (recorder != null && _uiState.value.status == RecordingStatus.PAUSED) {
                try {
                    recorder.resume()
                    startAmplitudeTracking()
                    _uiState.update {
                        it.copy(
                            status = RecordingStatus.RECORDING,
                            statusText = "Recording in progress",
                            micConnectedText = "Capturing clearly · Microphone connected",
                            audioLevelCategory = "Capturing..."
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Resume failed: ${e.localizedMessage ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    fun onStopClicked(context: Context? = null) {
        synchronized(recorderLock) {
            timerJob?.cancel()
            timerJob = null
            amplitudeJob?.cancel()
            amplitudeJob = null

            val recorder = mediaRecorder
            if (recorder != null) {
                try {
                    recorder.stop()
                } catch (_: RuntimeException) {
                    // Thrown if stop is called immediately without captured data
                }
                try {
                    recorder.reset()
                    recorder.release()
                } catch (_: Exception) {}
            }
            mediaRecorder = null
            isRecordingSessionActive = false

            // Verify file exists and has non-zero size
            val file = currentOutputFile
            if (file != null && file.exists() && file.length() >= 1024L && elapsedSeconds >= 1L) {
                val sizeBytes = file.length()
                val formattedSize = formatBytes(sizeBytes)
                val durationText = _uiState.value.timerText
                val title = _uiState.value.lectureTitle.ifBlank { "Database Normalization" }

                // Save audio reference and selected language in SessionManager
                SessionManager.setLastRecordedAudioPath(file.absolutePath)
                SessionManager.setSelectedLanguage(_uiState.value.selectedLanguageCode)
                SessionManager.setLastUploadedAudio(
                    audioId = SessionManager.getLastUploadedAudioId() ?: 0,
                    title = title,
                    duration = durationText
                )

                _uiState.update {
                    it.copy(
                        status = RecordingStatus.STOPPED,
                        statusText = "Recording saved ($formattedSize)",
                        micConnectedText = "Audio saved locally · Tap 'Next' to transcribe & generate AI notes",
                        recordedFilePath = file.absolutePath,
                        fileSizeBytes = sizeBytes,
                        formattedFileSize = formattedSize,
                        errorMessage = null,
                        audioLevelCategory = "Saved",
                        audioAmplitudes = List(20) { 0.35f } // Clean static waveform on completion
                    )
                }

                // If context provided, upload audio to PHP backend in background
                if (context != null) {
                    uploadRecordedAudio(context, file, title, durationText)
                }
            } else {
                // Clean up empty / corrupt file
                try {
                    file?.delete()
                } catch (_: Exception) {}
                currentOutputFile = null

                _uiState.update {
                    it.copy(
                        status = RecordingStatus.IDLE,
                        statusText = "Recording cancelled",
                        errorMessage = "Recording was too short or empty. Please speak into the microphone and record again.",
                        recordedFilePath = null,
                        fileSizeBytes = 0L,
                        audioAmplitudes = List(20) { 0.15f },
                        audioLevelCategory = "Ready"
                    )
                }
            }
        }
    }

    private fun uploadRecordedAudio(context: Context, file: File, title: String, duration: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val uploadResult = AudioRepository.uploadAudio(
                    context = context,
                    uri = Uri.fromFile(file),
                    fileName = file.name,
                    fileSize = file.length(),
                    title = title,
                    duration = duration
                )
                if (uploadResult.isSuccess && uploadResult.audioId != null) {
                    SessionManager.setLastUploadedAudio(
                        audioId = uploadResult.audioId,
                        title = title,
                        duration = duration
                    )
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadedAudioId = uploadResult.audioId
                        )
                    }
                } else {
                    _uiState.update { it.copy(isUploading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                isPermissionDenied = true,
                errorMessage = "Microphone permission is required to record lectures. Please tap 'Grant Permission' below or enable microphone access in Android settings."
            )
        }
    }

    fun onPermissionGranted(context: Context) {
        _uiState.update {
            it.copy(
                isPermissionDenied = false,
                errorMessage = null
            )
        }
        startRecording(context)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Safely handles screen disposal or lifecycle backgrounding.
     * If user leaves the screen while recording has captured meaningful audio (>2s),
     * the audio is safely saved so lecture data is not lost.
     */
    fun onScreenDisposed(context: Context? = null) {
        synchronized(recorderLock) {
            if (isRecordingSessionActive) {
                if (elapsedSeconds >= 2L && currentOutputFile != null) {
                    onStopClicked(context)
                } else {
                    releaseRecorderInternal()
                    _uiState.update {
                        it.copy(
                            status = RecordingStatus.IDLE,
                            statusText = "Recording stopped"
                        )
                    }
                }
            }
        }
    }

    private fun releaseRecorderInternal() {
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecordingSessionActive = false
    }

    override fun onCleared() {
        super.onCleared()
        releaseRecorderInternal()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
