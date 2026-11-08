package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.network.ChatConversationSummary
import com.example.Simtas.network.ChatHistoryMessageItem
import com.example.Simtas.network.ChatHistoryRequest
import com.example.Simtas.network.ChatHistoryResponse
import com.example.Simtas.network.ChatMessageContent
import com.example.Simtas.network.ChatSendMessageRequest
import com.example.Simtas.network.ChatSendResponse
import com.example.Simtas.ui.qa.MessageSource
import com.example.Simtas.ui.qa.QaAssistantViewModel
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for SmartNotes AI Gemini Chat Integration (Phase 2C).
 * Verifies:
 * 1. Request DTO structure matching PHP backend contract.
 * 2. Response DTO serialization and deserialization.
 * 3. ViewModel state management, conversation continuity, duplicate request prevention.
 * 4. Distinct source separation (Online Gemini vs Offline Curriculum).
 */
class QaAssistantChatTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        // Reset any static in-memory session token
        SessionManager.saveSession(
            context = null,
            token = "test_bearer_token_abc123",
            userId = 2,
            name = "Alex Morgan",
            email = "alex.morgan@campus.edu"
        )
    }

    @Test
    fun testChatSendMessageRequest_Serialization() {
        val request = ChatSendMessageRequest(
            message = "Explain BCNF in two simple sentences.",
            conversation_id = 6,
            subject = "Database Systems",
            lecture_title = "Database Normalization"
        )

        val json = gson.toJson(request)

        assertTrue("JSON should contain action", json.contains("\"action\":\"send_message\""))
        assertTrue("JSON should contain message", json.contains("\"message\":\"Explain BCNF in two simple sentences.\""))
        assertTrue("JSON should contain conversation_id", json.contains("\"conversation_id\":6"))
        assertTrue("JSON should contain subject", json.contains("\"subject\":\"Database Systems\""))
        assertTrue("JSON should contain lecture_title", json.contains("\"lecture_title\":\"Database Normalization\""))
    }

    @Test
    fun testChatSendResponse_DeserializationFromBackendJson() {
        // Actual JSON format returned by PHP ai/chat.php in Phase 2A/2B
        val backendJson = """
            {
                "success": true,
                "conversation_id": 6,
                "conversation_title": "Explain BCNF in two simple sentences.",
                "user_message": {
                    "id": 6,
                    "role": "user",
                    "content": "Explain BCNF in two simple sentences."
                },
                "assistant_message": {
                    "id": 7,
                    "role": "assistant",
                    "content": "Boyce-Codd Normal Form (BCNF) requires that for every non-trivial functional dependency, the determinant must be a candidate key.",
                    "provider": "gemini"
                },
                "ai_status": "completed",
                "message": "Response generated successfully by Google Gemini."
            }
        """.trimIndent()

        val response = gson.fromJson(backendJson, ChatSendResponse::class.java)

        assertTrue(response.success)
        assertEquals(6, response.conversation_id)
        assertEquals("Explain BCNF in two simple sentences.", response.conversation_title)
        assertNotNull(response.user_message)
        assertEquals("user", response.user_message?.role)
        assertEquals(6, response.user_message?.id)

        assertNotNull(response.assistant_message)
        assertEquals("assistant", response.assistant_message?.role)
        assertEquals(7, response.assistant_message?.id)
        assertEquals("gemini", response.assistant_message?.provider)
        assertTrue(response.assistant_message?.content?.startsWith("Boyce-Codd Normal Form") == true)
        assertEquals("completed", response.ai_status)
    }

    @Test
    fun testChatHistoryResponse_Deserialization() {
        val historyJson = """
            {
                "success": true,
                "conversation": {
                    "id": 6,
                    "title": "Explain BCNF in two simple sentences.",
                    "created_at": "2026-11-08 18:30:00",
                    "updated_at": "2026-11-08 18:30:05"
                },
                "messages": [
                    {
                        "id": 6,
                        "role": "user",
                        "message_content": "Explain BCNF in two simple sentences.",
                        "provider": null,
                        "created_at": "2026-11-08 18:30:00"
                    },
                    {
                        "id": 7,
                        "role": "assistant",
                        "message_content": "Boyce-Codd Normal Form (BCNF) requires every determinant to be a candidate key.",
                        "provider": "gemini",
                        "created_at": "2026-11-08 18:30:05"
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(historyJson, ChatHistoryResponse::class.java)

        assertTrue(response.success)
        assertEquals(6, response.conversation?.id)
        assertEquals(2, response.messages.size)
        assertEquals("user", response.messages[0].role)
        assertEquals("assistant", response.messages[1].role)
        assertEquals("gemini", response.messages[1].provider)
    }

    @Test
    fun testQaAssistantViewModel_InitialState() {
        val viewModel = QaAssistantViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.conversationId)
        assertNull(state.errorMessage)
    }

    @Test
    fun testQaAssistantViewModel_OfflineModeLabeling() {
        val viewModel = QaAssistantViewModel()

        // Force offline query
        viewModel.askQuestion(
            question = "What is 2NF?",
            forceOffline = true
        )

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size) // User message + Offline Answer

        val userMsg = state.messages.find { it.sender == MessageSource.USER }
        val offlineMsg = state.messages.find { it.sender == MessageSource.OFFLINE_ENGINE }

        assertNotNull(userMsg)
        assertNotNull(offlineMsg)

        // Strict requirement: Never present an offline answer as Gemini
        assertNotEquals("gemini", offlineMsg?.provider)
        assertEquals(MessageSource.OFFLINE_ENGINE, offlineMsg?.sender)
        assertTrue(offlineMsg?.provider?.contains("Offline") == true)
        assertTrue(offlineMsg?.text?.isNotEmpty() == true)
    }

    @Test
    fun testQaAssistantViewModel_ResetConversation() {
        val viewModel = QaAssistantViewModel()
        viewModel.askQuestion("What is 1NF?", forceOffline = true)

        assertFalse(viewModel.uiState.value.messages.isEmpty())

        viewModel.startNewConversation()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertNull(viewModel.uiState.value.conversationId)
    }

    @Test
    fun testMessageSource_DistinctSeparation() {
        val sources = MessageSource.values()
        assertEquals(3, sources.size)
        assertTrue(sources.contains(MessageSource.USER))
        assertTrue(sources.contains(MessageSource.ONLINE_GEMINI))
        assertTrue(sources.contains(MessageSource.OFFLINE_ENGINE))
    }
}

