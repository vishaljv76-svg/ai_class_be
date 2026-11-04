package com.example.Simtas.ui.subjects

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [SubjectSelectionScreen] adhering to MVVM architecture.
 */
class SubjectSelectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectSelectionUiState())
    val uiState: StateFlow<SubjectSelectionUiState> = _uiState.asStateFlow()

    fun toggleSubjectSelection(subjectId: String) {
        _uiState.update { state ->
            val updatedSubjects = state.subjects.map { subject ->
                if (subject.id == subjectId) {
                    subject.copy(isSelected = !subject.isSelected)
                } else {
                    subject
                }
            }
            state.copy(subjects = updatedSubjects)
        }
    }

    fun onStartLearningClicked() {
        _uiState.update { it.copy(isLoading = true) }
        // Process final subjects onboarding completion
    }
}
