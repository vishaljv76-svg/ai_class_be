package com.example.Simtas.ui.subjects

enum class SubjectIconType {
    ComputerScience,
    Mathematics,
    Physics,
    Chemistry,
    English
}

data class SubjectItem(
    val id: String,
    val title: String,
    val description: String,
    val iconType: SubjectIconType,
    val isSelected: Boolean = false
)

/**
 * UI State for Subject Selection ("Choose subjects") Screen following MVVM architecture.
 */
data class SubjectSelectionUiState(
    val subjects: List<SubjectItem> = listOf(
        SubjectItem("1", "Computer Science", "Code, systems & databases", SubjectIconType.ComputerScience, isSelected = true),
        SubjectItem("2", "Mathematics", "Equations, logic & problem solving", SubjectIconType.Mathematics, isSelected = true),
        SubjectItem("3", "Physics", "Matter, motion & energy", SubjectIconType.Physics, isSelected = false),
        SubjectItem("4", "Chemistry", "Elements, reactions & discovery", SubjectIconType.Chemistry, isSelected = false),
        SubjectItem("5", "English", "Language, literature & expression", SubjectIconType.English, isSelected = false)
    ),
    val currentStep: Int = 2,
    val totalSteps: Int = 2,
    val isLoading: Boolean = false
) {
    val selectedCount: Int
        get() = subjects.count { it.isSelected }
}
