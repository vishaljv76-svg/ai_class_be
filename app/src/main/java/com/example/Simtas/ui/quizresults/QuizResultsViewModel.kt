package com.example.Simtas.ui.quizresults

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for [QuizResultsScreen] adhering to MVVM architecture.
 */
class QuizResultsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizResultsUiState())
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    fun onRetryQuizClicked() {
        // Retry quiz logic
    }

    fun onBackToNotesClicked() {
        // Return to notes logic
    }
}
