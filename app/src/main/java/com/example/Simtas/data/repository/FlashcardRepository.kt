package com.example.Simtas.data.repository

import com.example.Simtas.data.local.FlashcardDeck
import com.example.Simtas.data.local.FlashcardItem
import com.example.Simtas.data.local.FlashcardStorage
import com.example.Simtas.data.remote.ApiClient
import com.example.Simtas.data.remote.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class FlashcardResult {
    data class Success(val deck: FlashcardDeck) : FlashcardResult()
    data class Error(val message: String, val errorCode: String? = null) : FlashcardResult()
}

/**
 * Repository for AI Smart Flashcards.
 * Connects to PHP backend (Google Gemini) and persists decks in FlashcardStorage.
 */
object FlashcardRepository {

    suspend fun generateFlashcards(
        topic: String,
        numCards: Int = 10
    ): FlashcardResult = withContext(Dispatchers.IO) {
        val trimmedTopic = topic.trim()
        if (trimmedTopic.isBlank()) {
            return@withContext FlashcardResult.Error("Please enter a subject or topic to generate flashcards.")
        }

        val userId = SessionManager.getUserId()
        val payload = JSONObject().apply {
            put("topic", trimmedTopic)
            put("subject", trimmedTopic)
            put("num_cards", numCards)
            put("numberOfCards", numCards)
            put("user_id", userId)
            put("userId", userId)
        }.toString()

        val response = ApiClient.post("flashcards/generate_flashcards.php", payload)
        if (response.isSuccess && response.jsonObject != null) {
            val root = response.jsonObject
            if (root.optBoolean("success", false)) {
                val deckTitle = root.optString("deck_title", "$trimmedTopic Flashcards")
                val subject = root.optString("subject", trimmedTopic)
                val topicName = root.optString("topic", trimmedTopic)
                val cardsList = mutableListOf<FlashcardItem>()

                val cardsArray = root.optJSONArray("flashcards")
                if (cardsArray != null) {
                    for (i in 0 until cardsArray.length()) {
                        val cardObj = cardsArray.optJSONObject(i) ?: continue
                        cardsList.add(
                            FlashcardItem(
                                cardNumber = cardObj.optInt("card_number", i + 1),
                                front = cardObj.optString("front", "Question ${i + 1}"),
                                back = cardObj.optString("back", "Answer ${i + 1}"),
                                hint = cardObj.optString("hint", topicName)
                            )
                        )
                    }
                }

                if (cardsList.isNotEmpty()) {
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                    val deck = FlashcardDeck(
                        id = "deck_" + System.currentTimeMillis(),
                        title = deckTitle,
                        subject = subject,
                        topic = topicName,
                        createdAt = dateStr,
                        cards = cardsList,
                        userId = userId
                    )
                    FlashcardStorage.saveDeck(deck)
                    return@withContext FlashcardResult.Success(deck)
                }
            } else {
                val errorMsg = root.optString("message", "Failed to generate flashcards.")
                return@withContext FlashcardResult.Error(errorMsg)
            }
        }

        // Offline / Fallback Generation: Generate from local saved notes if available
        val offlineDeck = generateOfflineFlashcards(trimmedTopic, numCards, userId)
        if (offlineDeck != null) {
            FlashcardStorage.saveDeck(offlineDeck)
            return@withContext FlashcardResult.Success(offlineDeck)
        }

        FlashcardResult.Error(response.errorMessage ?: "Unable to connect to SmartNotes AI service. Please check your connection.")
    }

    fun getSavedDecks(): List<FlashcardDeck> {
        val userId = SessionManager.getUserId()
        return FlashcardStorage.getAllDecks(userId)
    }

    fun getDeck(deckId: String): FlashcardDeck? {
        return FlashcardStorage.getDeck(deckId)
    }

    fun updateCardReview(deckId: String, cardNumber: Int, isKnown: Boolean): FlashcardDeck? {
        return FlashcardStorage.updateCardReview(deckId, cardNumber, isKnown)
    }

    fun deleteDeck(deckId: String): Boolean {
        return FlashcardStorage.deleteDeck(deckId)
    }

    fun generateOfflineFlashcards(topic: String, numCards: Int, userId: Int = SessionManager.getUserId()): FlashcardDeck? {
        val notes = com.example.Simtas.data.local.LectureNotesStorage.getAllNotesForList(userId)
        val matchingNote = notes.firstOrNull { 
            it.title.contains(topic, ignoreCase = true) || it.subject.contains(topic, ignoreCase = true) 
        } ?: notes.firstOrNull()

        if (matchingNote != null) {
            val savedLecture = com.example.Simtas.data.local.LectureNotesStorage.getSavedLecture(matchingNote.id)
            if (savedLecture != null) {
                val takeaways = savedLecture.generatedNotes.keyTakeaways
                val detectedQA = savedLecture.generatedNotes.detectedQuestionsAndAnswers
                val cards = mutableListOf<FlashcardItem>()

                for ((idx, qa) in detectedQA.withIndex()) {
                    if (cards.size >= numCards) break
                    cards.add(
                        FlashcardItem(
                            cardNumber = cards.size + 1,
                            front = qa.question,
                            back = qa.answer,
                            hint = matchingNote.subject
                        )
                    )
                }

                for ((idx, takeaway) in takeaways.withIndex()) {
                    if (cards.size >= numCards) break
                    cards.add(
                        FlashcardItem(
                            cardNumber = cards.size + 1,
                            front = "Core Takeaway #${idx + 1} regarding ${matchingNote.title}",
                            back = takeaway,
                            hint = matchingNote.subject
                        )
                    )
                }

                if (cards.isNotEmpty()) {
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                    return FlashcardDeck(
                        id = "deck_offline_" + System.currentTimeMillis(),
                        title = "$topic Flashcards (Study Notes)",
                        subject = matchingNote.subject,
                        topic = topic,
                        createdAt = dateStr,
                        cards = cards,
                        userId = userId
                    )
                }
            }
        }

        // Foundational concept fallback when no local lecture notes exist
        val defaultCards = listOf(
            FlashcardItem(
                cardNumber = 1,
                front = "What is the primary definition and core concept of $topic?",
                back = "Foundational principles, terminology, and underlying mechanics of $topic.",
                hint = topic
            ),
            FlashcardItem(
                cardNumber = 2,
                front = "What are the key architectural components of $topic?",
                back = "The fundamental building blocks, workflows, and standards governing $topic.",
                hint = topic
            ),
            FlashcardItem(
                cardNumber = 3,
                front = "What are the main advantages and practical applications of $topic?",
                back = "Practical industry use cases, performance benefits, and modern problem solving with $topic.",
                hint = topic
            )
        ).take(numCards.coerceAtLeast(1))

        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        return FlashcardDeck(
            id = "deck_offline_" + System.currentTimeMillis(),
            title = "$topic Flashcards",
            subject = topic,
            topic = topic,
            createdAt = dateStr,
            cards = defaultCards,
            userId = userId
        )
    }
}
