package com.example.Simtas.ui.quizresults

import androidx.lifecycle.ViewModel
import com.example.Simtas.data.remote.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for [QuizResultsScreen] adhering to MVVM architecture with real quiz result loading.
 */
class QuizResultsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizResultsUiState())
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    fun loadResults() {
        val cached = SessionManager.getLastQuizResult()
        if (cached != null) {
            _uiState.value = cached
        }
    }

    fun onRetryQuizClicked() {
        // Retry quiz logic
    }

    fun onBackToNotesClicked() {
        // Return to notes logic
    }
}
