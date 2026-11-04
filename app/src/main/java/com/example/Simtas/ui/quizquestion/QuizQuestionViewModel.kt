package com.example.Simtas.ui.quizquestion

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [QuizQuestionScreen] adhering to MVVM architecture.
 */
class QuizQuestionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizQuestionUiState())
    val uiState: StateFlow<QuizQuestionUiState> = _uiState.asStateFlow()

    fun selectOption(optionKey: String) {
        _uiState.update {
            it.copy(
                selectedOptionKey = optionKey,
                statusNoteText = "You've selected $optionKey · You can change your answer"
            )
        }
    }

    fun onNextClicked() {
        val current = _uiState.value.currentQuestionIndex
        val total = _uiState.value.totalQuestions
        if (current < total) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = current + 1,
                    selectedOptionKey = null,
                    statusNoteText = "Select an option to proceed"
                )
            }
        } else {
            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }
}
