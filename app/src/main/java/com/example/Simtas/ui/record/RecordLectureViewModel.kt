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
 * ViewModel for [RecordLectureScreen] adhering to MVVM architecture.
 * Manages audio recording lifecycle with MediaRecorder, permission validation,
 * private storage file handling, and background upload.
 */
class RecordLectureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RecordLectureUiState())
    val uiState: StateFlow<RecordLectureUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private var elapsedSeconds: Long = 0L
    private val recorderLock = Any()

    @Volatile
    private var isRecordingSessionActive: Boolean = false

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

            try {
                // 1. Prepare private storage directory for audio recordings
                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs()
                }

                // 2. Generate unique filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(recordingsDir, "lecture_rec_${timestamp}.m4a")
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
                        recordedFilePath = null,
                        fileSizeBytes = 0L,
                        formattedFileSize = ""
                    )
                }

                startTimer()

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

    fun onPauseClicked() {
        synchronized(recorderLock) {
            val recorder = mediaRecorder
            if (recorder != null && _uiState.value.status == RecordingStatus.RECORDING) {
                try {
                    recorder.pause()
                    _uiState.update {
                        it.copy(
                            status = RecordingStatus.PAUSED,
                            statusText = "Recording paused",
                            micConnectedText = "Microphone paused · Tap Resume to continue"
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
                    _uiState.update {
                        it.copy(
                            status = RecordingStatus.RECORDING,
                            statusText = "Recording in progress",
                            micConnectedText = "Capturing clearly · Microphone connected"
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

            val recorder = mediaRecorder
            if (recorder != null) {
                try {
                    recorder.stop()
                } catch (e: RuntimeException) {
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
            if (file != null && file.exists() && file.length() > 0L) {
                val sizeBytes = file.length()
                val formattedSize = formatBytes(sizeBytes)
                val durationText = _uiState.value.timerText
                val title = _uiState.value.lectureTitle.ifBlank { "Database Normalization" }

                // Save audio reference in SessionManager
                SessionManager.setLastRecordedAudioPath(file.absolutePath)
                SessionManager.setLastUploadedAudio(
                    audioId = SessionManager.getLastUploadedAudioId() ?: 0,
                    title = title,
                    duration = durationText
                )

                _uiState.update {
                    it.copy(
                        status = RecordingStatus.STOPPED,
                        statusText = "Recording saved ($formattedSize)",
                        micConnectedText = "Audio saved locally · Tap Next to proceed",
                        recordedFilePath = file.absolutePath,
                        fileSizeBytes = sizeBytes,
                        formattedFileSize = formattedSize,
                        errorMessage = null
                    )
                }

                // If context provided, upload audio to PHP backend in background
                if (context != null) {
                    uploadRecordedAudio(context, file, title, durationText)
                }
            } else {
                _uiState.update {
                    it.copy(
                        status = RecordingStatus.IDLE,
                        statusText = "Recording cancelled or empty",
                        errorMessage = "Recording was too short or empty. Please record again.",
                        recordedFilePath = null,
                        fileSizeBytes = 0L
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
                errorMessage = "Microphone permission is required to record lectures. Please allow microphone access in settings."
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onScreenDisposed() {
        synchronized(recorderLock) {
            if (isRecordingSessionActive) {
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

    private fun releaseRecorderInternal() {
        timerJob?.cancel()
        timerJob = null
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
