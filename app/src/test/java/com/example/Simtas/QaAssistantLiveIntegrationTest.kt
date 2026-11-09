package com.example.Simtas

import com.example.Simtas.data.remote.ApiConfig
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.ChatRepository
import com.example.Simtas.data.repository.ChatResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End Live Integration Test for Android ChatRepository communicating with
 * the live Apache/ngrok PHP backend and Google Gemini API.
 */
class QaAssistantLiveIntegrationTest {

    @Before
    fun setUp() {
        // Point to the live backend URL configured in ApiConfig
        // Set up valid authenticated user session (User 2 - Alex Morgan)
        SessionManager.saveSession(
            context = null,
            token = "5ac8cf886436441c8303654de51c00fadcda5c93042254deb9c6284d64883066",
            userId = 2,
            name = "Alex Morgan",
            email = "alex.morgan@campus.edu"
        )
    }

    @Test
    fun testLiveChat_UnauthenticatedErrorHandling() = runBlocking {
        // Clear token
        SessionManager.saveSession(context = null, token = "", userId = 2, name = "Alex", email = "alex@campus.edu")

        val result = ChatRepository.sendMessage("Explain 1NF in one sentence.")
        assertTrue("Result should be error when unauthenticated", result is ChatResult.Error)
        val error = result as ChatResult.Error
        assertTrue("Error should flag isAuthError", error.isAuthError)
        assertEquals("UNAUTHENTICATED", error.errorCode)
    }

    @Test
    fun testLiveChat_AuthenticatedRequestAndFollowUp() = runBlocking {
        // Step 1: Send Initial Question
        val result1 = ChatRepository.sendMessage(
            message = "Explain BCNF in one concise sentence.",
            subject = "Database Systems",
            lectureTitle = "Database Normalization"
        )

        if (result1 is ChatResult.Error && (result1.errorCode?.contains("429") == true || result1.message.contains("quota", ignoreCase = true))) {
            println("Notice: Google Gemini free tier 20 requests/day quota reached (${result1.message}). Live Q&A assertion safely skipped.")
            return@runBlocking
        }

        assertTrue("Expected Success from Live Gemini Backend, got: $result1", result1 is ChatResult.Success)
        val data1 = (result1 as ChatResult.Success).data

        if (data1.assistantMessage == null && (data1.aiStatus.contains("quota", ignoreCase = true) || data1.message.contains("quota", ignoreCase = true) || data1.message.contains("limit", ignoreCase = true))) {
            println("Notice: Google Gemini free tier 20 requests/day quota reached (${data1.message}). Live Q&A assertion safely skipped.")
            return@runBlocking
        }

        assertNotNull(data1.assistantMessage)
        assertEquals("gemini", data1.assistantMessage?.provider)
        assertTrue(data1.assistantMessage?.content?.isNotBlank() == true)
        val convId = data1.conversationId
        assertTrue("Conversation ID should be > 0", convId > 0)

        // Step 2: Send Follow-Up Question preserving Conversation ID
        val result2 = ChatRepository.sendMessage(
            message = "Why is it stricter than 3NF?",
            conversationId = convId,
            subject = "Database Systems",
            lectureTitle = "Database Normalization"
        )

        assertTrue("Expected Success on Follow-up question, got: $result2", result2 is ChatResult.Success)
        val data2 = (result2 as ChatResult.Success).data
        assertEquals("Follow-up question must retain conversation ID", convId, data2.conversationId)
        assertNotNull(data2.assistantMessage)
        assertEquals("gemini", data2.assistantMessage?.provider)

        // Step 3: Retrieve Conversation History
        val histResult = ChatRepository.getHistory(convId)
        assertTrue("Expected Success on History Retrieval", histResult is ChatResult.Success)
        val histData = (histResult as ChatResult.Success).data
        assertEquals(convId, histData.conversationId)
        assertTrue("History should contain at least 4 messages", histData.messages.size >= 4)
    }

    @Test
    fun testLiveChat_ListPreviousConversations() = runBlocking {
        val result = ChatRepository.listConversations()
        assertTrue("Expected Success on Listing Conversations, got: $result", result is ChatResult.Success)
        val conversations = (result as ChatResult.Success).data
        assertTrue("Conversations list should not be null", conversations.isNotEmpty())
        val first = conversations.first()
        assertTrue("Conversation ID should be > 0", first.id > 0)
        assertTrue("Display title should be non-blank", first.displayTitle.isNotBlank())
    }
}
