package com.example.Simtas.ui.preview

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * ViewModel for [LecturePreviewScreen] adhering to MVVM architecture.
 * Manages audio reference resolution, actual media playback using MediaPlayer,
 * speed toggling, and metadata updates.
 */
class LecturePreviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LecturePreviewUiState())
    val uiState: StateFlow<LecturePreviewUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentSpeed: Float = 1.0f

    init {
        // Initial check from SessionManager
        loadAudioFromSession()
    }

    fun loadAudio(path: String? = null, context: Context? = null) {
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                applyAudioFile(file)
                return
            }
        }

        // Try SessionManager
        val sessionPath = SessionManager.getLastRecordedAudioPath()
        if (!sessionPath.isNullOrBlank()) {
            val file = File(sessionPath)
            if (file.exists() && file.length() > 0L) {
                applyAudioFile(file)
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
                    applyAudioFile(latest)
                    return
                }
            }
        }

        // If no file found, provide helpful status
        _uiState.update {
            it.copy(
                statusText = "No lecture audio loaded",
                fileMetadataText = "Please record a lecture first",
                hasAudioFile = false
            )
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
    }

    private fun applyAudioFile(file: File) {
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
                fileMetadataText = "${file.name} · $sizeFormatted · Ready to transcribe",
                statusText = "Ready to transcribe",
                errorMessage = null
            )
        }
    }

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

            // Apply playback speed
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

    fun onGenerateTranscriptClicked() {
        pauseAudio()
    }

    fun onScreenDisposed() {
        releasePlayer()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
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
