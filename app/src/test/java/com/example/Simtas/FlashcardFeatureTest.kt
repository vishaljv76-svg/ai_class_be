package com.example.Simtas

import com.example.Simtas.data.local.FlashcardDeck
import com.example.Simtas.data.local.FlashcardItem
import com.example.Simtas.data.local.FlashcardStorage
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.FlashcardRepository
import com.example.Simtas.ui.flashcards.FlashcardViewMode
import com.example.Simtas.ui.flashcards.FlashcardsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Tests for SmartNotes AI - AI Smart Flashcards Feature.
 * Tests local persistence, ViewModel state transitions, card flipping, Know/Don't Know tracking,
 * mastery calculation, reviewing missed cards, and offline fallback generation.
 */
class FlashcardFeatureTest {

    @Before
    fun setUp() {
        FlashcardStorage.clearAll()
        SessionManager.clearSession()
    }

    @After
    fun tearDown() {
        FlashcardStorage.clearAll()
        SessionManager.clearSession()
    }

    @Test
    fun test1_flashcardStorage_saveAndRetrieveDeckInMemory() {
        val sampleCards = listOf(
            FlashcardItem(cardNumber = 1, front = "What is TCP?", back = "Transmission Control Protocol", hint = "Layer 4"),
            FlashcardItem(cardNumber = 2, front = "What is UDP?", back = "User Datagram Protocol", hint = "Connectionless")
        )
        val deck = FlashcardDeck(
            id = "deck_test_1",
            title = "Computer Networks Flashcards",
            subject = "Computer Networks",
            topic = "Transport Layer",
            createdAt = "Nov 18, 2026",
            cards = sampleCards
        )

        FlashcardStorage.saveDeck(deck)
        val retrieved = FlashcardStorage.getDeck("deck_test_1")

        assertNotNull(retrieved)
        assertEquals("deck_test_1", retrieved?.id)
        assertEquals("Computer Networks Flashcards", retrieved?.title)
        assertEquals("Transport Layer", retrieved?.topic)
        assertEquals(2, retrieved?.cards?.size)
        assertEquals("What is TCP?", retrieved?.cards?.get(0)?.front)
        assertEquals("Transmission Control Protocol", retrieved?.cards?.get(0)?.back)
        assertEquals("Layer 4", retrieved?.cards?.get(0)?.hint)
    }

    @Test
    fun test2_flashcardStorage_updateCardReviewState() {
        val sampleCards = listOf(
            FlashcardItem(cardNumber = 1, front = "What is Polymorphism?", back = "Many forms"),
            FlashcardItem(cardNumber = 2, front = "What is Encapsulation?", back = "Data hiding")
        )
        val deck = FlashcardDeck(
            id = "deck_oop",
            title = "OOP Flashcards",
            subject = "Computer Science",
            topic = "OOP Concepts",
            createdAt = "Nov 18, 2026",
            cards = sampleCards
        )
        FlashcardStorage.saveDeck(deck)

        // Mark card 1 as Known
        val after1 = FlashcardStorage.updateCardReview("deck_oop", cardNumber = 1, isKnown = true)
        assertNotNull(after1)
        assertEquals(true, after1?.cards?.find { it.cardNumber == 1 }?.isKnown)
        assertEquals(1, after1?.knownCount)
        assertEquals(0, after1?.unknownCount)

        // Mark card 2 as Not Known
        val after2 = FlashcardStorage.updateCardReview("deck_oop", cardNumber = 2, isKnown = false)
        assertNotNull(after2)
        assertEquals(false, after2?.cards?.find { it.cardNumber == 2 }?.isKnown)
        assertEquals(1, after2?.knownCount)
        assertEquals(1, after2?.unknownCount)

        // Verify retrieval matches updated state
        val reloaded = FlashcardStorage.getDeck("deck_oop")
        assertEquals(1, reloaded?.knownCount)
        assertEquals(1, reloaded?.unknownCount)
    }

    @Test
    fun test3_flashcardStorage_deleteDeck() {
        val deck = FlashcardDeck(
            id = "deck_delete_me",
            title = "OS Deck",
            subject = "Operating Systems",
            topic = "Deadlocks",
            createdAt = "Nov 18, 2026",
            cards = listOf(FlashcardItem(cardNumber = 1, front = "Kernel?", back = "Core of OS"))
        )
        FlashcardStorage.saveDeck(deck)
        assertTrue(FlashcardStorage.getAllDecks().any { it.id == "deck_delete_me" })

        FlashcardStorage.deleteDeck("deck_delete_me")
        assertFalse(FlashcardStorage.getAllDecks().any { it.id == "deck_delete_me" })
        assertNull(FlashcardStorage.getDeck("deck_delete_me"))
    }

    @Test
    fun test4_viewModel_topicInputAndCardCount() {
        val viewModel = FlashcardsViewModel()
        assertEquals("", viewModel.uiState.value.topicInput)

        viewModel.onTopicInputChanged("Python Programming")
        assertEquals("Python Programming", viewModel.uiState.value.topicInput)

        viewModel.onNumCardsChanged(15)
        assertEquals(15, viewModel.uiState.value.numCards)
    }

    @Test
    fun test5_viewModel_emptyTopicShowsError() {
        val viewModel = FlashcardsViewModel()
        viewModel.onTopicInputChanged("")
        viewModel.generateFlashcards()

        assertEquals("Please enter a subject or topic name.", viewModel.uiState.value.errorMessage)
        assertEquals(FlashcardViewMode.CREATION, viewModel.uiState.value.viewMode)
    }

    @Test
    fun test6_viewModel_studySessionCardFlipAndNavigation() {
        val testCards = listOf(
            FlashcardItem(cardNumber = 1, front = "DNS stands for?", back = "Domain Name System", hint = "Internet phonebook"),
            FlashcardItem(cardNumber = 2, front = "HTTP port?", back = "80", hint = "Well known port"),
            FlashcardItem(cardNumber = 3, front = "HTTPS port?", back = "443", hint = "Secure port")
        )
        val deck = FlashcardDeck(
            id = "deck_session",
            title = "Web Protocols",
            subject = "Computer Networks",
            topic = "Protocols",
            createdAt = "Nov 18, 2026",
            cards = testCards
        )
        FlashcardStorage.saveDeck(deck)

        val viewModel = FlashcardsViewModel()
        viewModel.startDeck(deck)

        val state1 = viewModel.uiState.value
        assertEquals(FlashcardViewMode.STUDY_SESSION, state1.viewMode)
        assertEquals(0, state1.currentCardIndex)
        assertEquals(3, state1.totalCards)
        assertFalse(state1.isCardFlipped)

        // Reveal answer (flip)
        viewModel.flipCard()
        assertTrue(viewModel.uiState.value.isCardFlipped)

        // Flip back
        viewModel.flipCard()
        assertFalse(viewModel.uiState.value.isCardFlipped)

        // Mark Know on Card 1 -> should advance to Card 2, reset flip
        viewModel.markCard(isKnown = true)
        val state2 = viewModel.uiState.value
        assertEquals(1, state2.currentCardIndex)
        assertEquals(1, state2.reviewKnownCount)
        assertEquals(0, state2.reviewUnknownCount)
        assertFalse(state2.isCardFlipped)

        // Mark Don't Know on Card 2 -> should advance to Card 3
        viewModel.markCard(isKnown = false)
        val state3 = viewModel.uiState.value
        assertEquals(2, state3.currentCardIndex)
        assertEquals(1, state3.reviewKnownCount)
        assertEquals(1, state3.reviewUnknownCount)

        // Mark Know on Card 3 (last card) -> should complete the session
        viewModel.markCard(isKnown = true)
        val stateCompleted = viewModel.uiState.value
        assertEquals(FlashcardViewMode.DECK_COMPLETED, stateCompleted.viewMode)
        assertEquals(2, stateCompleted.reviewKnownCount)
        assertEquals(1, stateCompleted.reviewUnknownCount)
    }

    @Test
    fun test7_viewModel_reviewMissedCardsOnly() {
        val testCards = listOf(
            FlashcardItem(cardNumber = 1, front = "Q1", back = "A1"),
            FlashcardItem(cardNumber = 2, front = "Q2", back = "A2"),
            FlashcardItem(cardNumber = 3, front = "Q3", back = "A3")
        )
        val deck = FlashcardDeck(
            id = "deck_review",
            title = "Review Test",
            subject = "Testing",
            topic = "Unit Tests",
            createdAt = "Nov 18, 2026",
            cards = testCards
        )
        FlashcardStorage.saveDeck(deck)

        val viewModel = FlashcardsViewModel()
        viewModel.startDeck(deck)

        // Card 1: Know
        viewModel.markCard(isKnown = true)
        // Card 2: Don't Know
        viewModel.markCard(isKnown = false)
        // Card 3: Don't Know
        viewModel.markCard(isKnown = false)

        assertEquals(FlashcardViewMode.DECK_COMPLETED, viewModel.uiState.value.viewMode)

        // Review Missed Cards
        viewModel.reviewMissedOnly()
        val missedSession = viewModel.uiState.value
        assertEquals(FlashcardViewMode.STUDY_SESSION, missedSession.viewMode)
        assertEquals(2, missedSession.activeDeck?.cards?.size)
        assertEquals("Q2", missedSession.currentCard?.front)
    }

    @Test
    fun test8_viewModel_exitToDecksList() {
        val deck = FlashcardDeck(
            id = "d1",
            title = "Physics Deck",
            subject = "Physics",
            topic = "Mechanics",
            createdAt = "Nov 18, 2026",
            cards = listOf(FlashcardItem(cardNumber = 1, front = "F=?", back = "ma"))
        )
        FlashcardStorage.saveDeck(deck)

        val viewModel = FlashcardsViewModel()
        viewModel.startDeck(deck)
        assertEquals(FlashcardViewMode.STUDY_SESSION, viewModel.uiState.value.viewMode)

        viewModel.returnToCreation()
        assertEquals(FlashcardViewMode.CREATION, viewModel.uiState.value.viewMode)
        assertFalse(viewModel.uiState.value.savedDecks.isEmpty())
    }

    @Test
    fun test9_offlineFallbackFlashcardsGeneration() {
        val fallbackDeck = FlashcardRepository.generateOfflineFlashcards("Database Systems", 5, userId = 1)
        assertNotNull(fallbackDeck)
        assertEquals("Database Systems", fallbackDeck?.subject)
        assertTrue(fallbackDeck?.cards?.isNotEmpty() == true)
        assertTrue(fallbackDeck!!.cards.all { it.front.isNotBlank() && it.back.isNotBlank() })
    }
}
