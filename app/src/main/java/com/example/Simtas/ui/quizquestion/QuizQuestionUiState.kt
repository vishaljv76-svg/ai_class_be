package com.example.Simtas.ui.quizquestion

data class QuizOptionItem(
    val optionKey: String,
    val text: String
)

/**
 * UI State for Quiz Question ("Knowledge check") Screen following MVVM architecture.
 */
data class QuizQuestionUiState(
    val currentQuestionIndex: Int = 1,
    val totalQuestions: Int = 5,
    val modeText: String = "Moderate · MCQ",
    val topicText: String = "Database Normalization · Computer Science",
    val questionText: String = "Which change is required to bring a 1NF table into 2NF?",
    val instructionText: String = "Choose one answer.",
    val options: List<QuizOptionItem> = listOf(
        QuizOptionItem("A", "Store multiple values in one cell"),
        QuizOptionItem("B", "Remove partial dependencies on a composite key"),
        QuizOptionItem("C", "Remove all transitive dependencies"),
        QuizOptionItem("D", "Merge every relation into one table")
    ),
    val selectedOptionKey: String? = "B",
    val statusNoteText: String = "You've selected B · You can change your answer",
    val isQuizFinished: Boolean = false
)
