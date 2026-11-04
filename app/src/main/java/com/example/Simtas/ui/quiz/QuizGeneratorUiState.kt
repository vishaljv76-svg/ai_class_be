package com.example.Simtas.ui.quiz

enum class QuestionType {
    MCQ,
    TRUE_FALSE,
    MIXED
}

enum class DifficultyLevel {
    EASY,
    MODERATE,
    HARD
}

/**
 * UI State for AI Quiz Generator Screen following MVVM architecture.
 */
data class QuizGeneratorUiState(
    val selectedSubject: String = "Computer Science - CS301",
    val subjectOptions: List<String> = listOf("Computer Science - CS301", "Mathematics - MATH201", "Physics - PHYS101"),
    val lectureTitle: String = "Database Normalization",
    val lectureMetadata: String = "21 Oct 2026 · Prof. Sarah Chen",
    val numberOfQuestions: Int = 5,
    val questionType: QuestionType = QuestionType.MCQ,
    val difficulty: DifficultyLevel = DifficultyLevel.MODERATE,
    val estimatedTimeText: String = "5 multiple-choice questions · About 5 min",
    val isLoading: Boolean = false
)
