package com.example.Simtas.ui.quizquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.QuizRepository
import com.example.Simtas.data.repository.RealQuizQuestion
import com.example.Simtas.ui.quizresults.AnswerReviewItem
import com.example.Simtas.ui.quizresults.QuizResultsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [QuizQuestionScreen] adhering to MVVM architecture with real questions and answers.
 */
class QuizQuestionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizQuestionUiState())
    val uiState: StateFlow<QuizQuestionUiState> = _uiState.asStateFlow()

    private var realQuestions: List<RealQuizQuestion> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, String>()
    private var activeQuizId: Int? = null

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        val quizId = SessionManager.getLastQuizId()
        val noteId = SessionManager.getLastNoteId()

        viewModelScope.launch {
            val result = QuizRepository.getQuiz(quizId = quizId, noteId = noteId)
            if (result.isSuccess && result.questions.isNotEmpty()) {
                realQuestions = result.questions
                activeQuizId = result.quizId
                val firstQ = realQuestions.first()

                _uiState.update {
                    it.copy(
                        currentQuestionIndex = 1,
                        totalQuestions = realQuestions.size,
                        modeText = "${result.difficulty} · MCQ",
                        topicText = firstQ.topicText,
                        questionText = firstQ.questionText,
                        instructionText = firstQ.instructionText,
                        options = firstQ.options,
                        selectedOptionKey = null,
                        statusNoteText = "Select an option to proceed",
                        isQuizFinished = false
                    )
                }
            }
        }
    }

    fun selectOption(optionKey: String) {
        val currentIndex = _uiState.value.currentQuestionIndex
        selectedAnswers[currentIndex] = optionKey
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
            val nextIndex = current + 1
            if (realQuestions.size >= nextIndex) {
                val nextQ = realQuestions[nextIndex - 1]
                val prevSelected = selectedAnswers[nextIndex]
                _uiState.update {
                    it.copy(
                        currentQuestionIndex = nextIndex,
                        topicText = nextQ.topicText,
                        questionText = nextQ.questionText,
                        instructionText = nextQ.instructionText,
                        options = nextQ.options,
                        selectedOptionKey = prevSelected,
                        statusNoteText = if (prevSelected != null) "You've selected $prevSelected · You can change your answer" else "Select an option to proceed"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentQuestionIndex = nextIndex,
                        selectedOptionKey = null,
                        statusNoteText = "Select an option to proceed"
                    )
                }
            }
        } else {
            // Quiz completed! Calculate score and submit
            finishQuizAndComputeResults()
        }
    }

    private fun finishQuizAndComputeResults() {
        viewModelScope.launch {
            val quizId = activeQuizId ?: SessionManager.getLastQuizId()

            if (quizId != null && quizId > 0 && selectedAnswers.isNotEmpty()) {
                // Submit to backend
                val subResult = QuizRepository.submitQuiz(quizId, selectedAnswers)
                if (subResult.isSuccess) {
                    val resultUi = QuizResultsUiState(
                        titleMessage = if (subResult.scorePercentage >= 70) "You're getting the big picture!" else "Keep practicing to strengthen your foundation.",
                        scorePercentage = subResult.scorePercentage,
                        correctCount = subResult.correctCount,
                        totalQuestions = subResult.totalQuestions,
                        quizTopicMetadata = "${_uiState.value.topicText} · ${_uiState.value.modeText}",
                        feedbackHeader = subResult.feedbackHeader.ifBlank { "Quiz Summary" },
                        feedbackText = subResult.feedbackText,
                        reviewItems = subResult.reviewItems
                    )
                    SessionManager.setLastQuizResult(resultUi)
                } else {
                    computeLocalScoreFallback()
                }
            } else {
                computeLocalScoreFallback()
            }

            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }

    private fun computeLocalScoreFallback() {
        var correctCount = 0
        val reviewItems = mutableListOf<AnswerReviewItem>()

        if (realQuestions.isNotEmpty()) {
            realQuestions.forEachIndexed { idx, q ->
                val qNum = idx + 1
                val chosen = selectedAnswers[qNum]
                val isCorrect = chosen?.equals(q.correctOption, ignoreCase = true) == true
                if (isCorrect) correctCount++

                val userOptText = q.options.firstOrNull { it.optionKey.equals(chosen, true) }?.text
                val correctOptText = q.options.firstOrNull { it.optionKey.equals(q.correctOption, true) }?.text

                reviewItems.add(
                    AnswerReviewItem(
                        questionNumber = "Q$qNum",
                        questionTopic = q.topic,
                        isCorrect = isCorrect,
                        userAnswerText = userOptText,
                        correctAnswerText = correctOptText,
                        explanationText = q.explanation.ifBlank { "Option ${q.correctOption} is the correct answer according to lecture principles." }
                    )
                )
            }
        }

        val total = if (realQuestions.isNotEmpty()) realQuestions.size else _uiState.value.totalQuestions
        val pct = if (total > 0) ((correctCount * 100) / total) else 80

        val fallbackUi = QuizResultsUiState(
            titleMessage = if (pct >= 70) "You're getting the big picture." else "Good effort · Review the key concepts below.",
            scorePercentage = pct,
            correctCount = correctCount,
            totalQuestions = total,
            quizTopicMetadata = "${_uiState.value.topicText} · ${_uiState.value.modeText}",
            feedbackHeader = if (pct >= 80) "A strong foundation" else "Areas for review",
            feedbackText = "You understand the core principles. Revisit the areas highlighted in the breakdown below.",
            reviewItems = if (reviewItems.isNotEmpty()) reviewItems else listOf(
                AnswerReviewItem("Q1", "Moving to 2NF", true, null, null, "Every non-key attribute must depend on the whole candidate key.")
            )
        )
        SessionManager.setLastQuizResult(fallbackUi)
    }
}
