package com.example.Simtas.ui.quiz

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [QuizGeneratorScreen] adhering to MVVM architecture.
 */
class QuizGeneratorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizGeneratorUiState())
    val uiState: StateFlow<QuizGeneratorUiState> = _uiState.asStateFlow()

    fun onSubjectSelected(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun onNumberOfQuestionsSelected(count: Int) {
        _uiState.update {
            it.copy(
                numberOfQuestions = count,
                estimatedTimeText = "$count multiple-choice questions · About $count min"
            )
        }
    }

    fun onQuestionTypeSelected(type: QuestionType) {
        _uiState.update { it.copy(questionType = type) }
    }

    fun onDifficultySelected(difficulty: DifficultyLevel) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onGenerateQuizClicked() {
        _uiState.update { it.copy(isLoading = true) }
        // Process AI quiz generation
    }
}
