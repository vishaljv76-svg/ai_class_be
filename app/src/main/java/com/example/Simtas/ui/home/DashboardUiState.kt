package com.example.Simtas.ui.home

enum class RecentNoteIconType {
    Database,
    Math
}

data class RecentNoteItem(
    val id: String,
    val title: String,
    val subject: String,
    val date: String,
    val duration: String,
    val iconType: RecentNoteIconType
)

enum class HomeBottomTab {
    Home,
    Notes,
    Profile
}

/**
 * UI State for the Home Dashboard Screen following MVVM architecture.
 */
data class DashboardUiState(
    val userName: String = "Alex",
    val dateText: String = "Wednesday, 21 October",
    val userInitials: String = "AM",
    val totalNotes: Int = 24,
    val subjectsCount: Int = 2,
    val quizzesCount: Int = 8,
    val recentNotes: List<RecentNoteItem> = listOf(
        RecentNoteItem("1", "Database Normalization", "Computer Science", "Today", "42 min", RecentNoteIconType.Database),
        RecentNoteItem("2", "Eigenvalues & Eigenvectors", "Mathematics", "20 Oct", "36 min", RecentNoteIconType.Math)
    ),
    val selectedTab: HomeBottomTab = HomeBottomTab.Home,
    val isLoading: Boolean = false
)
