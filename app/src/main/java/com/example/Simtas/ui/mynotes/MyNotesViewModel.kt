package com.example.Simtas.ui.mynotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.NotesRepositoryImpl
import com.example.Simtas.ui.home.HomeBottomTab
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
}
