package com.example.Simtas.ui.preview

import androidx.lifecycle.ViewModel
import com.example.Simtas.data.remote.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [LecturePreviewScreen] adhering to MVVM architecture.
 */
class LecturePreviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LecturePreviewUiState())
    val uiState: StateFlow<LecturePreviewUiState> = _uiState.asStateFlow()

    init {
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
    }

    fun onLectureTitleChanged(title: String) {
        _uiState.update { it.copy(lectureTitle = title) }
    }

    fun onSubjectSelected(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun onRewind10s() {
        // Rewind 10s
    }

    fun onForward10s() {
        // Forward 10s
    }

    fun onSpeedClicked() {
        _uiState.update { state ->
            val nextSpeed = when (state.playbackSpeedText) {
                "1x" -> "1.25x"
                "1.25x" -> "1.5x"
                "1.5x" -> "2x"
                else -> "1x"
            }
            state.copy(playbackSpeedText = nextSpeed)
        }
    }

    fun onGenerateTranscriptClicked() {
        _uiState.update { it.copy(isLoading = true) }
        // Process transcript generation
    }
}
