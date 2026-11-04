package com.example.Simtas.ui.onboarding

/**
 * UI State for Student Onboarding ("Your study profile") Screen following MVVM architecture.
 */
data class StudentOnboardingUiState(
    val studentName: String = "Alex Morgan",
    val selectedCourse: String = "B.Tech · Computer Science",
    val selectedDepartment: String = "Computer Science & Engineering",
    val selectedSemester: String = "Semester 7 · Final year",
    val selectedSubjects: List<String> = listOf("Computer Science", "Mathematics"),
    val courseOptions: List<String> = listOf("B.Tech · Computer Science", "B.Tech · Electrical Engineering", "B.Tech · Mechanical Engineering"),
    val departmentOptions: List<String> = listOf("Computer Science & Engineering", "Information Technology", "Electronics & Communication"),
    val semesterOptions: List<String> = listOf("Semester 1 · 1st year", "Semester 3 · 2nd year", "Semester 5 · 3rd year", "Semester 7 · Final year"),
    val currentStep: Int = 1,
    val totalSteps: Int = 2,
    val isLoading: Boolean = false
)
