package com.example.Simtas.ui.detailednotes

/**
 * UI State for Detailed Notes Screen following MVVM architecture.
 */
data class DetailedNotesUiState(
    val courseCode: String = "CS301",
    val tagText: String = "AI notes",
    val lectureTitle: String = "Database Normalization",
    val metadataText: String = "Prof. Sarah Chen · 21 Oct 2026 · 6 min read",
    val section1Title: String = "01 Why normalize?",
    val section1Content: String = "Normalization organizes relations to minimize repeated data. It protects consistency when records are inserted, updated or deleted.",
    val section2Title: String = "02 The first three normal forms",
    val section2Bullets: List<String> = listOf(
        "1NF — Atomic values in each cell; no repeating groups.",
        "2NF — In 1NF, and every non-key attribute depends on the whole candidate key.",
        "3NF — In 2NF, with no transitive dependency between non-key attributes."
    ),
    val calloutTitle: String = "Functional dependency: X → Y",
    val calloutContent: String = "Each value of X determines exactly one value of Y.\nExample: StudentID → StudentName.",
    val section3Title: String = "03 Worked example",
    val section3Content1: String = "Enrollment(StudentID, CourseID, StudentName, Grade) has key (StudentID, CourseID). StudentName depends only on StudentID: a partial dependency.",
    val section3Content2: String = "Split into Student(StudentID, StudentName) and Enrollment(StudentID, CourseID, Grade) to reach 2NF.",
    val footerText: String = "Excerpt · Next: 3NF decomposition & practice",
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = false
)
