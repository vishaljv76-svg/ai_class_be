package com.example.Simtas.ui.mynotes

import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.home.RecentNoteIconType

data class NoteListItem(
    val id: String,
    val title: String,
    val subject: String,
    val dateText: String,
    val durationText: String,
    val readTimeText: String,
    val iconType: RecentNoteIconType
)

/**
 * UI State for My Notes Screen following MVVM architecture.
 */
data class MyNotesUiState(
    val mainHeadline: String = "Your semester, sorted.",
    val statsSubtitle: String = "24 notes · 2 subjects · A little more clarity",
    val searchQuery: String = "",
    val selectedCategoryIndex: Int = 0,
    val monthSectionTitle: String = "October 2026",
    val recentNotesCountText: String = "3 recent notes",
    val notes: List<NoteListItem> = listOf(
        NoteListItem("1", "Database Normalization", "Computer Science", "21 Oct", "42 min", "6 min read", RecentNoteIconType.Database),
        NoteListItem("2", "Eigenvalues & Eigenvectors", "Mathematics", "20 Oct", "36 min", "5 min read", RecentNoteIconType.Math),
        NoteListItem("3", "SQL Joins & Subqueries", "Computer Science", "19 Oct", "48 min", "7 min read", RecentNoteIconType.Database)
    ),
    val backupStatusText: String = "All notes backed up · Updated just now",
    val selectedTab: HomeBottomTab = HomeBottomTab.Notes
)
