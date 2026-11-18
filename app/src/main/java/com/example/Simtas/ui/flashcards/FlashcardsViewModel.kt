package com.example.Simtas.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.local.FlashcardDeck
import com.example.Simtas.data.local.FlashcardItem
import com.example.Simtas.data.repository.FlashcardRepository
import com.example.Simtas.data.repository.FlashcardResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardsViewModel(
    private val repository: FlashcardRepository = FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardsUiState())
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    init {
        loadSavedDecks()
    }

    fun loadSavedDecks() {
        val decks = repository.getSavedDecks()
        _uiState.update { it.copy(savedDecks = decks) }
    }

    fun onTopicInputChanged(topic: String) {
        _uiState.update { it.copy(topicInput = topic, errorMessage = null) }
    }

    fun onNumCardsChanged(numCards: Int) {
        _uiState.update { it.copy(numCards = numCards) }
    }

    fun generateFlashcards() {
        val topic = _uiState.value.topicInput.trim()
        if (topic.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a subject or topic name.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.generateFlashcards(topic, _uiState.value.numCards)) {
                is FlashcardResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            activeDeck = result.deck,
                            currentCardIndex = 0,
                            isCardFlipped = false,
                            viewMode = FlashcardViewMode.STUDY_SESSION,
                            reviewKnownCount = 0,
                            reviewUnknownCount = 0,
                            savedDecks = repository.getSavedDecks()
                        )
                    }
                }
                is FlashcardResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun startDeck(deck: FlashcardDeck) {
        _uiState.update {
            it.copy(
                activeDeck = deck,
                currentCardIndex = 0,
                isCardFlipped = false,
                viewMode = FlashcardViewMode.STUDY_SESSION,
                reviewKnownCount = deck.knownCount,
                reviewUnknownCount = deck.unknownCount,
                errorMessage = null
            )
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun markCard(isKnown: Boolean) {
        val state = _uiState.value
        val deck = state.activeDeck ?: return
        val currentCard = state.currentCard ?: return

        // Update persistent deck
        val updatedDeck = repository.updateCardReview(deck.id, currentCard.cardNumber, isKnown) ?: deck

        val newKnownCount = if (isKnown) state.reviewKnownCount + 1 else state.reviewKnownCount
        val newUnknownCount = if (!isKnown) state.reviewUnknownCount + 1 else state.reviewUnknownCount

        val nextIndex = state.currentCardIndex + 1
        if (nextIndex >= state.totalCards) {
            // Deck completed
            _uiState.update {
                it.copy(
                    activeDeck = updatedDeck,
                    viewMode = FlashcardViewMode.DECK_COMPLETED,
                    isCardFlipped = false,
                    reviewKnownCount = newKnownCount,
                    reviewUnknownCount = newUnknownCount,
                    savedDecks = repository.getSavedDecks()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    activeDeck = updatedDeck,
                    currentCardIndex = nextIndex,
                    isCardFlipped = false,
                    reviewKnownCount = newKnownCount,
                    reviewUnknownCount = newUnknownCount
                )
            }
        }
    }

    fun previousCard() {
        val state = _uiState.value
        if (state.currentCardIndex > 0) {
            _uiState.update {
                it.copy(
                    currentCardIndex = state.currentCardIndex - 1,
                    isCardFlipped = false
                )
            }
        }
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.currentCardIndex < state.totalCards - 1) {
            _uiState.update {
                it.copy(
                    currentCardIndex = state.currentCardIndex + 1,
                    isCardFlipped = false
                )
            }
        }
    }

    fun restartDeck() {
        _uiState.update {
            it.copy(
                currentCardIndex = 0,
                isCardFlipped = false,
                viewMode = FlashcardViewMode.STUDY_SESSION,
                reviewKnownCount = 0,
                reviewUnknownCount = 0
            )
        }
    }

    fun reviewMissedOnly() {
        val state = _uiState.value
        val deck = state.activeDeck ?: return
        val missedCards = deck.cards.filter { it.isKnown == false }
        if (missedCards.isEmpty()) {
            restartDeck()
            return
        }

        val filteredDeck = deck.copy(
            cards = missedCards.mapIndexed { i, c -> c.copy(cardNumber = i + 1, isKnown = null) }
        )
        _uiState.update {
            it.copy(
                activeDeck = filteredDeck,
                currentCardIndex = 0,
                isCardFlipped = false,
                viewMode = FlashcardViewMode.STUDY_SESSION,
                reviewKnownCount = 0,
                reviewUnknownCount = 0
            )
        }
    }

    fun returnToCreation() {
        _uiState.update {
            it.copy(
                viewMode = FlashcardViewMode.CREATION,
                activeDeck = null,
                isCardFlipped = false,
                errorMessage = null,
                savedDecks = repository.getSavedDecks()
            )
        }
    }

    fun deleteDeck(deckId: String) {
        repository.deleteDeck(deckId)
        loadSavedDecks()
    }
}
