package com.example.Simtas.ui.quizresults

import androidx.lifecycle.ViewModel
import com.example.Simtas.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for [QuizResultsScreen] adhering to MVVM architecture.
 */
class QuizResultsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizResultsUiState())
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    fun loadResults() {
        val submission = QuizRepository.cachedSubmissionResult
        val activeQuiz = QuizRepository.cachedActiveQuiz

        if (submission != null && submission.isSuccess) {
            val titleMsg = when {
                submission.scorePercentage >= 80 -> "You're getting the big picture."
                submission.scorePercentage >= 60 -> "Solid progress on this topic."
                else -> "Keep practicing."
            }
            val metadata = "${activeQuiz?.title ?: "AI Quiz"} · ${activeQuiz?.difficulty ?: "Moderate"} · ${submission.totalQuestions} Questions"

            _uiState.update {
                it.copy(
                    titleMessage = titleMsg,
                    scorePercentage = submission.scorePercentage,
                    correctCount = submission.correctCount,
                    totalQuestions = submission.totalQuestions,
                    quizTopicMetadata = metadata,
                    feedbackHeader = submission.feedbackHeader.ifBlank { "Quiz Summary" },
                    feedbackText = submission.feedbackText.ifBlank { "You completed the quiz." },
                    reviewItems = submission.reviewItems,
                    isLoading = false
                )
            }
        }
    }

    fun onRetryQuizClicked() {
        // Clear previous submission so retake starts fresh
        QuizRepository.cachedSubmissionResult = null
    }

    fun onBackToNotesClicked() {
        // Handled by navigation
    }
}
