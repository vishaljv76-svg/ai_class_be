package com.example.Simtas.ui.mynotes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.NotesRepositoryImpl
import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.utils.ExportResult
import com.example.Simtas.utils.PdfExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [MyNotesScreen] adhering to MVVM architecture with real API notes persistence.
 */
class MyNotesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyNotesUiState())
    val uiState: StateFlow<MyNotesUiState> = _uiState.asStateFlow()

    private val categoryOptions = listOf("All", "Computer Science", "Mathematics", "Physics")

    init {
        loadNotes()
    }

    fun loadNotes() {
        val search = _uiState.value.searchQuery
        val catIndex = _uiState.value.selectedCategoryIndex
        val cat = categoryOptions.getOrElse(catIndex) { "All" }

        viewModelScope.launch {
            val result = NotesRepositoryImpl.fetchNotes(search = search, category = cat)
            _uiState.update { current ->
                current.copy(
                    statsSubtitle = result.statsSubtitle,
                    backupStatusText = result.backupStatusText,
                    notes = result.notes
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadNotes()
    }

    fun selectCategory(index: Int) {
        _uiState.update { it.copy(selectedCategoryIndex = index) }
        loadNotes()
    }

    fun selectTab(tab: HomeBottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onDeleteNote(noteId: String) {
        // Optimistically remove from UI
        _uiState.update { state ->
            state.copy(notes = state.notes.filter { it.id != noteId })
        }
        // Asynchronously delete in MySQL database
        viewModelScope.launch {
            NotesRepositoryImpl.deleteNote(noteId)
            loadNotes() // re-sync count and list
        }
    }

    /**
     * Exports a note to a formatted PDF and saves it to the Downloads directory.
     */
    fun exportNoteToPdf(
        context: Context,
        note: NoteListItem,
        onComplete: (Boolean, ExportResult?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val detailed = NotesRepositoryImpl.fetchNoteById(note.id)
                    ?: com.example.Simtas.data.local.LectureNotesStorage.getDetailedNotes(note.id)
                val pdfData = PdfExportManager.createPdfDataFromNoteItem(note, detailed)
                val result = PdfExportManager.exportNoteToPdf(context, pdfData)
                if (result.isSuccess) {
                    onComplete(true, result.getOrNull(), null)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "PDF export failed"
                    onComplete(false, null, err)
                }
            } catch (e: Exception) {
                onComplete(false, null, e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
