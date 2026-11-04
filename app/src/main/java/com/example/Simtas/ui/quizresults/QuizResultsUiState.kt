package com.example.Simtas.ui.quizresults

data class AnswerReviewItem(
    val questionNumber: String,
    val questionTopic: String,
    val isCorrect: Boolean,
    val userAnswerText: String? = null,
    val correctAnswerText: String? = null,
    val explanationText: String
)

/**
 * UI State for Quiz Results Screen following MVVM architecture.
 */
data class QuizResultsUiState(
    val titleMessage: String = "You're getting the big picture.",
    val scorePercentage: Int = 80,
    val correctCount: Int = 4,
    val totalQuestions: Int = 5,
    val quizTopicMetadata: String = "Database Normalization · Moderate · 5 MCQs",
    val feedbackHeader: String = "A strong foundation",
    val feedbackText: String = "You understand 1NF and 2NF. Revisit transitive dependencies to feel confident with 3NF.",
    val reviewItems: List<AnswerReviewItem> = listOf(
        AnswerReviewItem(
            questionNumber = "Q1",
            questionTopic = "Moving from 1NF to 2NF",
            isCorrect = true,
            explanationText = "B is correct. Every non-key attribute must depend on the whole candidate key, not only part of a composite key."
        ),
        AnswerReviewItem(
            questionNumber = "Q4",
            questionTopic = "Transitive dependencies",
            isCorrect = false,
            userAnswerText = "2NF",
            correctAnswerText = "3NF",
            explanationText = "3NF removes dependencies between non-key attributes. 2NF alone does not."
        )
    ),
    val isLoading: Boolean = false
)
