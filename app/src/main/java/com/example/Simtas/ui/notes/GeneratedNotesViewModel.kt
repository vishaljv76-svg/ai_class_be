package com.example.Simtas.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.AiNotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [GeneratedNotesScreen] adhering to MVVM architecture with real API saving.
 */
class GeneratedNotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratedNotesUiState())
    val uiState: StateFlow<GeneratedNotesUiState> = _uiState.asStateFlow()

    init {
        loadGeneratedNotes()
    }

    fun loadGeneratedNotes() {
        val cachedNote = SessionManager.getLastGeneratedNote()
        if (cachedNote != null) {
            _uiState.value = cachedNote
        } else {
            val lastTitle = SessionManager.getLastUploadedAudioTitle()
            if (!lastTitle.isNullOrBlank()) {
                _uiState.update { it.copy(lectureTitle = lastTitle) }
            }
        }
    }

    fun onViewFullNotesClicked() {
        // Open full notes view
    }

    fun onTakeQuizClicked() {
        // Open quiz screen
    }

    fun onSaveNotesClicked() {
        onSaveNotesClicked({})
    }

    fun onSaveNotesClicked(onSuccess: (String?) -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val tid = SessionManager.getLastTranscriptionId()
            val result = AiNotesRepository.saveGeneratedNote(
                transcriptionId = tid,
                uiState = _uiState.value
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
                onSuccess(result.noteId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(null)
            }
        }
    }
}
