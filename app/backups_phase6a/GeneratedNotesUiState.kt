package com.example.Simtas.ui.notes

/**
 * Verified educational reference or textbook discovered for this lecture.
 */
data class DiscoveredResourceItem(
    val id: Int = 0,
    val title: String,
    val url: String,
    val source: String = "Web",
    val topic: String? = null,
    val snippet: String? = null,
    val isVerified: Boolean = true
)

/**
 * Question detected in lecture audio, with offline generated answer and status.
 */
data class DetectedQuestionAnswerItem(
    val question: String,
    val answer: String,
    val explanation: String = "",
    val status: String = "Answered", // "Answered" or "Not found"
    val isAnswered: Boolean = true
)

/**
 * UI State for Generated Notes Screen following MVVM architecture.
 */
data class GeneratedNotesUiState(
    val noteId: Int? = null,
    val quizId: Int? = null,
    val subjectBadge: String = "Computer Science",
    val lectureTitle: String = "Database Normalization",
    val metadataText: String = "21 Oct 2026 · 42 min 18 sec · CS301",
    val aiSummaryText: String = "Normalization reduces redundancy and prevents data anomalies. Move from atomic values (1NF) to full-key dependency (2NF), then remove transitive dependencies (3NF).",
    val keyTakeaways: List<String> = listOf(
        "Repeated data can cause update, insertion and deletion anomalies.",
        "2NF removes partial dependencies on a composite key.",
        "3NF keeps non-key attributes dependent only on the key."
    ),
    val importantTerms: List<String> = listOf(
        "Functional dependency",
        "Composite key",
        "Normal forms"
    ),
    val educationalResources: List<DiscoveredResourceItem> = emptyList(),
    val detectedQuestionsAndAnswers: List<DetectedQuestionAnswerItem> = emptyList(),
    val mathQuestion: String? = null,
    val mathAnswer: String? = null,
    val mathExplanation: String? = null,
    val hasMathSolution: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)
