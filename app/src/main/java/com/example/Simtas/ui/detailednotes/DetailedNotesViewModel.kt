package com.example.Simtas.ui.detailednotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.NotesRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [DetailedNotesScreen] adhering to MVVM architecture with real API data.
 */
class DetailedNotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DetailedNotesUiState())
    val uiState: StateFlow<DetailedNotesUiState> = _uiState.asStateFlow()

    private var currentNoteId: String = "1"

    init {
        loadNote(currentNoteId)
    }

    fun loadNote(noteId: String) {
        currentNoteId = noteId
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 1. Check local persistent storage for this specific lecture
            val localDetailed = com.example.Simtas.data.local.LectureNotesStorage.getDetailedNotes(noteId)
            if (localDetailed != null) {
                _uiState.value = localDetailed
                com.example.Simtas.data.remote.SessionManager.setLastDetailedNotes(localDetailed)
                val localGen = com.example.Simtas.data.local.LectureNotesStorage.getGeneratedNotes(noteId)
                if (localGen != null) {
                    com.example.Simtas.data.remote.SessionManager.setLastGeneratedNote(localGen)
                }
                val localTranscript = com.example.Simtas.data.local.LectureNotesStorage.getTranscript(noteId)
                if (localTranscript != null) {
                    com.example.Simtas.data.remote.SessionManager.setLastTranscription(localTranscript, emptyList())
                }
                return@launch
            }

            // 2. Query PHP backend if available
            val note = NotesRepositoryImpl.fetchNoteById(noteId)
            if (note != null) {
                _uiState.value = note
                com.example.Simtas.data.remote.SessionManager.setLastDetailedNotes(note)
            } else {
                val sessionDetailed = com.example.Simtas.data.remote.SessionManager.getLastDetailedNotes()
                if (sessionDetailed != null) {
                    _uiState.value = sessionDetailed
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun toggleBookmark() {
        val newBookmarkState = !_uiState.value.isBookmarked
        _uiState.update { it.copy(isBookmarked = newBookmarkState) }

        viewModelScope.launch {
            NotesRepositoryImpl.toggleBookmark(currentNoteId, newBookmarkState)
        }
    }

    fun onDownloadPdfClicked() {
        _uiState.update { it.copy(isLoading = true) }
        // Process PDF export / download
    }
}
