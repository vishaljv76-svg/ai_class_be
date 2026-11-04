package com.example.Simtas.ui.createnotes

import com.example.Simtas.ui.home.HomeBottomTab

data class NoteFeatureItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isIncluded: Boolean = true
)

/**
 * UI State for Create Smart Notes Screen following MVVM architecture.
 */
data class CreateSmartNotesUiState(
    val mainTitle: String = "Connecting the ideas...",
    val subtitleText: String = "Turning Database Normalization into notes that make sense.",
    val currentStepText: String = "Structuring your notes",
    val progressPercent: Int = 72,
    val progressStatusText: String = "Transcript analyzed · Organizing key concepts",
    val includedFeatures: List<NoteFeatureItem> = listOf(
        NoteFeatureItem("1", "Short summary", "The big picture in a few sentences"),
        NoteFeatureItem("2", "Detailed notes", "Structured explanations and examples"),
        NoteFeatureItem("3", "Key points", "The ideas worth remembering"),
        NoteFeatureItem("4", "Definitions", "Important terms & concepts explained")
    ),
    val selectedTab: HomeBottomTab = HomeBottomTab.Home,
    val isGenerating: Boolean = false,
    val isGenerationComplete: Boolean = false,
    val errorMessage: String? = null
)
