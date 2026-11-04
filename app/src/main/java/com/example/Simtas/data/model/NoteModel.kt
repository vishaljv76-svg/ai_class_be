package com.example.Simtas.data.model

/**
 * Data model representing a class note.
 * Part of the MVVM data layer architecture for future screens.
 */
data class NoteModel(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val fullText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
