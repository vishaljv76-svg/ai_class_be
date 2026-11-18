package com.example.Simtas.ui.flashcards

import com.example.Simtas.data.local.FlashcardDeck
import com.example.Simtas.data.local.FlashcardItem

enum class FlashcardViewMode {
    CREATION,
    STUDY_SESSION,
    DECK_COMPLETED
}

data class FlashcardsUiState(
    val viewMode: FlashcardViewMode = FlashcardViewMode.CREATION,
    val topicInput: String = "",
    val numCards: Int = 10,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeDeck: FlashcardDeck? = null,
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val savedDecks: List<FlashcardDeck> = emptyList(),
    val reviewKnownCount: Int = 0,
    val reviewUnknownCount: Int = 0
) {
    val currentCard: FlashcardItem?
        get() = activeDeck?.cards?.getOrNull(currentCardIndex)

    val totalCards: Int
        get() = activeDeck?.cards?.size ?: 0

    val progressFraction: Float
        get() = if (totalCards > 0) (currentCardIndex + 1).toFloat() / totalCards.toFloat() else 0f
}
