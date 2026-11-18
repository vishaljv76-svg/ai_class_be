package com.example.Simtas.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class FlashcardItem(
    val cardNumber: Int,
    val front: String,
    val back: String,
    val hint: String = "",
    val isKnown: Boolean? = null // null: unreviewed, true: Know, false: Don't Know
)

data class FlashcardDeck(
    val id: String,
    val title: String,
    val subject: String,
    val topic: String,
    val createdAt: String,
    val cards: List<FlashcardItem>,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val userId: Int = 0
)

/**
 * Dedicated Persistent Storage for AI Smart Flashcards.
 * Dual-layer architecture:
 * 1. Concurrent in-memory map (instant UI reactivity & unit testable without Android mocks)
 * 2. Persistent SharedPreferences disk storage (persists decks across app restarts)
 */
object FlashcardStorage {

    private const val PREFS_NAME = "simtas_saved_flashcards"
    private const val KEY_DECK_IDS = "saved_deck_ids"
    private const val KEY_PREFIX_DECK = "deck_data_"

    private var appContext: Context? = null
    private val inMemoryDecks = ConcurrentHashMap<String, FlashcardDeck>()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadAllFromDisk()
    }

    /**
     * Saves a flashcard deck to in-memory map and persistent storage.
     */
    fun saveDeck(deck: FlashcardDeck): String {
        val id = if (deck.id.isNotBlank()) deck.id else "deck_" + System.currentTimeMillis()
        val formattedDeck = deck.copy(id = id)
        inMemoryDecks[id] = formattedDeck
        saveToDisk(formattedDeck)
        return id
    }

    /**
     * Retrieves a flashcard deck by its unique ID.
     */
    fun getDeck(deckId: String): FlashcardDeck? {
        return inMemoryDecks[deckId] ?: loadDeckFromDisk(deckId)?.also { inMemoryDecks[deckId] = it }
    }

    /**
     * Retrieves all saved flashcard decks belonging to the specified user.
     */
    fun getAllDecks(userId: Int = com.example.Simtas.data.remote.SessionManager.getUserId()): List<FlashcardDeck> {
        val all = inMemoryDecks.values.toList()
        return if (userId > 0) {
            all.filter { it.userId == 0 || it.userId == userId }
        } else {
            all
        }.sortedByDescending { it.id }
    }

    /**
     * Updates review status (Know / Don't Know) for a specific card in a deck.
     */
    fun updateCardReview(deckId: String, cardNumber: Int, isKnown: Boolean): FlashcardDeck? {
        val existing = getDeck(deckId) ?: return null
        val updatedCards = existing.cards.map { card ->
            if (card.cardNumber == cardNumber) {
                card.copy(isKnown = isKnown)
            } else {
                card
            }
        }
        val knownCount = updatedCards.count { it.isKnown == true }
        val unknownCount = updatedCards.count { it.isKnown == false }
        val updatedDeck = existing.copy(
            cards = updatedCards,
            knownCount = knownCount,
            unknownCount = unknownCount
        )
        inMemoryDecks[deckId] = updatedDeck
        saveToDisk(updatedDeck)
        return updatedDeck
    }

    /**
     * Deletes a deck from storage.
     */
    fun deleteDeck(deckId: String): Boolean {
        inMemoryDecks.remove(deckId)
        val ctx = appContext ?: return true
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idSet = prefs.getStringSet(KEY_DECK_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        idSet.remove(deckId)
        prefs.edit()
            .putStringSet(KEY_DECK_IDS, idSet)
            .remove(KEY_PREFIX_DECK + deckId)
            .apply()
        return true
    }

    /**
     * Clears in-memory storage (used for isolated testing).
     */
    fun clearAll() {
        inMemoryDecks.clear()
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun saveToDisk(deck: FlashcardDeck) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = serializeDeck(deck)
            val idSet = prefs.getStringSet(KEY_DECK_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            idSet.add(deck.id)

            prefs.edit()
                .putStringSet(KEY_DECK_IDS, idSet)
                .putString(KEY_PREFIX_DECK + deck.id, json)
                .apply()
        } catch (_: Exception) {}
    }

    private fun loadAllFromDisk() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val idSet = prefs.getStringSet(KEY_DECK_IDS, emptySet()) ?: emptySet()
            for (id in idSet) {
                loadDeckFromDisk(id)?.let { inMemoryDecks[it.id] = it }
            }
        } catch (_: Exception) {}
    }

    private fun loadDeckFromDisk(deckId: String): FlashcardDeck? {
        val ctx = appContext ?: return null
        return try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PREFIX_DECK + deckId, null) ?: return null
            deserializeDeck(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeDeck(deck: FlashcardDeck): String {
        val root = JSONObject()
        root.put("id", deck.id)
        root.put("title", deck.title)
        root.put("subject", deck.subject)
        root.put("topic", deck.topic)
        root.put("createdAt", deck.createdAt)
        root.put("knownCount", deck.knownCount)
        root.put("unknownCount", deck.unknownCount)
        root.put("userId", deck.userId)

        val cardsArray = JSONArray()
        for (card in deck.cards) {
            val cardObj = JSONObject()
            cardObj.put("cardNumber", card.cardNumber)
            cardObj.put("front", card.front)
            cardObj.put("back", card.back)
            cardObj.put("hint", card.hint)
            if (card.isKnown != null) {
                cardObj.put("isKnown", card.isKnown)
            }
            cardsArray.put(cardObj)
        }
        root.put("cards", cardsArray)
        return root.toString()
    }

    private fun deserializeDeck(jsonString: String): FlashcardDeck {
        val root = JSONObject(jsonString)
        val id = root.optString("id", "")
        val title = root.optString("title", "Flashcards")
        val subject = root.optString("subject", "General")
        val topic = root.optString("topic", "")
        val createdAt = root.optString("createdAt", "Recent")
        val knownCount = root.optInt("knownCount", 0)
        val unknownCount = root.optInt("unknownCount", 0)
        val userId = root.optInt("userId", 0)

        val cardsList = mutableListOf<FlashcardItem>()
        val cardsArray = root.optJSONArray("cards")
        if (cardsArray != null) {
            for (i in 0 until cardsArray.length()) {
                val c = cardsArray.optJSONObject(i) ?: continue
                val isKnownVal = if (c.has("isKnown")) c.getBoolean("isKnown") else null
                cardsList.add(
                    FlashcardItem(
                        cardNumber = c.optInt("cardNumber", i + 1),
                        front = c.optString("front", ""),
                        back = c.optString("back", ""),
                        hint = c.optString("hint", ""),
                        isKnown = isKnownVal
                    )
                )
            }
        }

        return FlashcardDeck(
            id = id,
            title = title,
            subject = subject,
            topic = topic,
            createdAt = createdAt,
            cards = cardsList,
            knownCount = knownCount,
            unknownCount = unknownCount,
            userId = userId
        )
    }
}
