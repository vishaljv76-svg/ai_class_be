package com.example.Simtas.ui.quiz

enum class QuestionType {
    MCQ,
    TRUE_FALSE,
    MIXED
}

enum class DifficultyLevel {
    EASY,
    MODERATE,
    HARD,
    MIXED
}

/**
 * UI State for AI Quiz Generator Screen following MVVM architecture.
 */
data class QuizGeneratorUiState(
    val topicInput: String = "",
    val selectedSubject: String = "All Saved My Notes",
    val subjectOptions: List<String> = listOf("All Saved My Notes"),
    val lectureTitle: String = "All My Notes",
    val lectureMetadata: String = "Comprehensive quiz based on your saved notes",
    val numberOfQuestions: Int = 10,
    val questionType: QuestionType = QuestionType.MCQ,
    val difficulty: DifficultyLevel = DifficultyLevel.MODERATE,
    val estimatedTimeText: String = "10 multiple-choice questions · About 10 min",
    val isLoading: Boolean = false,
    val savedNotes: List<com.example.Simtas.ui.mynotes.NoteListItem> = emptyList(),
    val selectedNoteIndex: Int = 0,
    val selectedNoteId: Int? = null,
    val selectedNoteKey: String? = null,
    val isMyNotesEmpty: Boolean = false,
    val errorMessage: String? = null
)
