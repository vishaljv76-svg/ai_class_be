package com.example.Simtas.ui.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [TranscriptScreen] adhering to MVVM architecture with real API transcription sync.
 */
class TranscriptViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    init {
        loadTranscription()
    }

    fun loadTranscription(audioId: Int? = SessionManager.getLastUploadedAudioId()) {
        val targetAudioId = audioId ?: SessionManager.getLastUploadedAudioId() ?: return
        val currentTitle = SessionManager.getLastUploadedAudioTitle()
        if (!currentTitle.isNullOrBlank()) {
            _uiState.update { it.copy(lectureTitle = currentTitle) }
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // 1. Try to fetch existing saved transcription from backend
            val result = AudioRepository.getTranscription(targetAudioId)
            if (result.isSuccess && result.paragraphs.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        lectureTitle = result.lectureTitle.ifBlank { it.lectureTitle },
                        language = result.language.ifBlank { it.language },
                        durationAndConfidence = result.durationAndConfidence.ifBlank { it.durationAndConfidence },
                        paragraphs = result.paragraphs,
                        isLoading = false
                    )
                }
                return@launch
            }

            // 2. If not found, attempt live speech-to-text service
            val liveResult = AudioRepository.transcribeAudio(targetAudioId)
            if (liveResult.isSuccess && liveResult.paragraphs.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        lectureTitle = liveResult.lectureTitle.ifBlank { it.lectureTitle },
                        durationAndConfidence = liveResult.durationAndConfidence.ifBlank { it.durationAndConfidence },
                        paragraphs = liveResult.paragraphs,
                        isLoading = false
                    )
                }
            } else {
                // If service is unconfigured or audio has no speech yet, retain structured preview and finish loading
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun onEditTextClicked() {
        // Edit transcript text
    }

    fun onGenerateNotesClicked() {
        _uiState.update { it.copy(isLoading = true) }
        val targetAudioId = SessionManager.getLastUploadedAudioId()
        if (targetAudioId != null) {
            viewModelScope.launch {
                // Persist current transcript so that the notes module has it ready
                val fullText = _uiState.value.paragraphs.joinToString("\n\n") { "${it.speaker}: ${it.text}" }
                AudioRepository.saveTranscription(
                    audioId = targetAudioId,
                    lectureTitle = _uiState.value.lectureTitle,
                    fullTranscript = fullText,
                    paragraphs = _uiState.value.paragraphs
                )
                _uiState.update { it.copy(isLoading = false) }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
