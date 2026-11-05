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
            val transcriptText = SessionManager.getLastTranscriptionText() ?: ""
            val paragraphs = SessionManager.getLastTranscriptionParagraphs()
            val lastTitle = SessionManager.getLastUploadedAudioTitle() ?: "Lecture Notes"
            val lastDuration = SessionManager.getLastUploadedAudioDuration() ?: "Recorded"

            if (transcriptText.isNotBlank() || paragraphs.isNotEmpty()) {
                val extractive = com.example.Simtas.data.ml.LectureNotesGenerator.generate(
                    transcriptText = transcriptText,
                    paragraphs = paragraphs,
                    lectureTitle = lastTitle,
                    subject = "Computer Science",
                    durationText = lastDuration
                )
                SessionManager.setLastGeneratedNote(extractive.generatedNotes)
                SessionManager.setLastDetailedNotes(extractive.detailedNotes)
                _uiState.value = extractive.generatedNotes
            } else {
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

            val currentDetailed = SessionManager.getLastDetailedNotes() ?: com.example.Simtas.ui.detailednotes.DetailedNotesUiState(lectureTitle = _uiState.value.lectureTitle)
            val transcript = SessionManager.getLastTranscriptionText() ?: ""
            val noteId = result.noteId ?: (tid?.toString() ?: (System.currentTimeMillis() / 1000).toString())

            // Persist locally so reopening the lecture loads its own saved notes
            com.example.Simtas.data.local.LectureNotesStorage.saveLecture(
                lectureId = noteId,
                title = _uiState.value.lectureTitle,
                subject = _uiState.value.subjectBadge,
                transcriptText = transcript,
                generatedNotes = _uiState.value.copy(isSaved = true),
                detailedNotes = currentDetailed
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
                onSuccess(result.noteId)
            } else {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
                onSuccess(noteId)
            }
        }
    }
}
