package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.network.ChatConversationSummary
import com.example.Simtas.network.ChatDeleteConversationRequest
import com.example.Simtas.network.ChatDeleteConversationResponse
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

    @Test
    fun testChatDeleteConversationRequest_Serialization() {
        val request = ChatDeleteConversationRequest(conversation_id = 42)
        val json = gson.toJson(request)
        assertTrue("JSON should contain action delete_conversation", json.contains("\"action\":\"delete_conversation\""))
        assertTrue("JSON should contain conversation_id 42", json.contains("\"conversation_id\":42"))
    }

    @Test
    fun testChatDeleteConversationResponse_Deserialization() {
        val backendJson = """
            {
                "success": true,
                "message": "Conversation deleted successfully.",
                "deleted_conversation_id": 42
            }
        """.trimIndent()
        val response = gson.fromJson(backendJson, ChatDeleteConversationResponse::class.java)
        assertTrue(response.success)
        assertEquals("Conversation deleted successfully.", response.message)
        assertEquals(42, response.deleted_conversation_id)
    }

    @Test
    fun testDirectQuestionAnsweringEngine_BcnfQueries() {
        val testQueries = listOf(
            "Explain BCNF normalization with a simple real-world example.",
            "Explain BCNF Normalization with a simple real-world example.",
            "What is BCNF?",
            "Explain machine learning with a real-life example.",
            "Explain deadlock in operating systems",
            "Explain ACID properties in database",
            "What is database normalization?",
            "Explain 1NF, 2NF and 3NF",
            "What is supervised learning?",
            "What is unsupervised learning?",
            "Explain photosynthesis"
        )

        for (q in testQueries) {
            val ans = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(q)
            println("QUERY: '$q' => isAnswered=${ans.isAnswered}, status=${ans.status}, answerPreview=${ans.answer.take(80)}")
            assertTrue("Query '$q' should be answered", ans.isAnswered)
            assertTrue("Query '$q' answer must not be empty", ans.answer.isNotBlank())
        }
    }

    @Test
    fun testBcnfNormalizationOfflineAnswer_HasRealWorldExample() {
        val q = "Explain BCNF Normalization with a simple real-world example."
        val ans = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(q)

        assertTrue("BCNF question must be answered", ans.isAnswered)
        assertTrue("Answer must contain Boyce-Codd Normal Form", ans.answer.contains("Boyce-Codd Normal Form"))
        assertTrue("Answer must contain Key Concepts", ans.answer.contains("Key Concepts"))
        assertTrue("Answer must contain Example", ans.answer.contains("Example:"))
        assertTrue("Answer must include student/professor/course example", ans.answer.contains("Advise(StudentID") || ans.answer.contains("Professor"))
        assertFalse("Explanation must not mention raw 429", ans.explanation.contains("429"))
    }

    @Test
    fun testMachineLearningSubtopics() {
        val qSupervised = "What is supervised learning?"
        val ansSupervised = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(qSupervised)
        assertTrue("Supervised learning must be answered", ansSupervised.isAnswered)
        assertTrue("Answer must contain classification or regression", ansSupervised.answer.contains("Classification") || ansSupervised.answer.contains("labeled"))

        val qUnsupervised = "What is unsupervised learning?"
        val ansUnsupervised = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(qUnsupervised)
        assertTrue("Unsupervised learning must be answered", ansUnsupervised.isAnswered)
        assertTrue("Answer must contain clustering or unlabeled", ansUnsupervised.answer.contains("Clustering") || ansUnsupervised.answer.contains("unlabeled"))

        val qML = "Explain machine learning with a real-life example."
        val ansML = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(qML)
        assertTrue("Machine learning must be answered", ansML.isAnswered)
        assertTrue("Answer must contain example", ansML.answer.contains("Example:"))
    }

    @Test
    fun testDatabaseNormalizationAndNormalForms() {
        val qNorm = "What is database normalization?"
        val ansNorm = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(qNorm)
        assertTrue(ansNorm.isAnswered)
        assertTrue(ansNorm.answer.contains("1NF") && ansNorm.answer.contains("BCNF"))

        val q1nf = "Explain 1NF with an example"
        val ans1nf = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(q1nf)
        assertTrue(ans1nf.isAnswered)
        assertTrue(ans1nf.answer.contains("atomic", ignoreCase = true))

        val q2nf = "Explain 2NF with an example"
        val ans2nf = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(q2nf)
        assertTrue(ans2nf.isAnswered)
        assertTrue(ans2nf.answer.contains("partial", ignoreCase = true))

        val q3nf = "Explain 3NF with an example"
        val ans3nf = com.example.Simtas.data.ml.QuestionAnsweringEngine.answerQuestion(q3nf)
        assertTrue(ans3nf.isAnswered)
        assertTrue(ans3nf.answer.contains("transitive", ignoreCase = true))
    }

    @Test
    fun testQaAssistantViewModel_GeminiRateLimitCooldownFlow() {
        val viewModel = QaAssistantViewModel()

        // Simulate active 60s cooldown from earlier Gemini 429
        viewModel.geminiRateLimitedUntil = System.currentTimeMillis() + 60_000L

        // Ask general academic question while rate-limited
        viewModel.askQuestion("Explain BCNF Normalization with a simple real-world example.")

        val state = viewModel.uiState.value
        val offlineMsg = state.messages.find { it.sender == MessageSource.OFFLINE_ENGINE }

        assertNotNull("Offline answer message should exist", offlineMsg)
        assertEquals("Answered using Offline Knowledge", offlineMsg?.provider)
        assertFalse("General question must not show Offline Curriculum", offlineMsg?.provider == "Offline Curriculum")
        assertNull("Error banner must NOT be shown when offline fallback successfully answers", state.errorMessage)
        assertFalse("Loading indicator must be stopped", state.isLoading)
        assertTrue("Must be marked answered", offlineMsg?.isAnswered == true)
        assertTrue("Must include BCNF example", offlineMsg?.text?.contains("Advise(StudentID") == true || offlineMsg?.text?.contains("Professor") == true)
    }

    @Test
    fun testQaAssistantViewModel_GeneralQuestionOfflineLabeling() {
        val viewModel = QaAssistantViewModel()

        // General query with NO lecture context (e.g. from AiChatScreen)
        viewModel.askQuestion(
            question = "Explain BCNF Normalization with a simple real-world example.",
            forceOffline = true
        )

        val state = viewModel.uiState.value
        val offlineMsg = state.messages.find { it.sender == MessageSource.OFFLINE_ENGINE }

        assertNotNull("Offline answer message should exist", offlineMsg)
        assertEquals("Answered using Offline Knowledge", offlineMsg?.provider)
        assertFalse("General question must not show Offline Curriculum", offlineMsg?.provider == "Offline Curriculum")
        assertNull("Error message must not be shown when fallback successfully answers", state.errorMessage)
        assertFalse("Loading indicator must be stopped", state.isLoading)
        assertTrue("Answer text must not be empty", offlineMsg?.text?.isNotEmpty() == true)
        assertTrue("Must be answered: actual text=${offlineMsg?.text}", offlineMsg?.isAnswered == true)
        assertFalse("Explanation must not contain raw HTTP 429 error code", offlineMsg?.explanation?.contains("429") == true)
    }

    @Test
    fun testQaAssistantViewModel_LectureQuestionOfflineLabeling() {
        val viewModel = QaAssistantViewModel()

        // Query WITH lecture context (e.g. from QaAssistantBottomSheet)
        viewModel.askQuestion(
            question = "What did the professor say about keys?",
            transcriptText = "In today's lecture on database design, the professor explained primary keys and foreign keys.",
            forceOffline = true
        )

        val state = viewModel.uiState.value
        val offlineMsg = state.messages.find { it.sender == MessageSource.OFFLINE_ENGINE }

        assertNotNull(offlineMsg)
        assertEquals("Offline Curriculum", offlineMsg?.provider)
        assertNull("No error toast when answered", state.errorMessage)
        assertFalse("Loading indicator must be stopped", state.isLoading)
    }

    @Test
    fun testQaAssistantViewModel_GeneralQuestionBothFailFriendlyMessage() {
        val viewModel = QaAssistantViewModel()

        // Unsupported random question without lecture context
        viewModel.askQuestion(
            question = "xyzqprrandomnonexistenttopic12345",
            forceOffline = true
        )

        val state = viewModel.uiState.value
        val offlineMsg = state.messages.find { it.sender == MessageSource.OFFLINE_ENGINE }

        assertNotNull(offlineMsg)
        assertFalse("Loading indicator must always be stopped", state.isLoading)
        assertFalse("General question must not claim not covered in lecture", offlineMsg?.text?.contains("not covered in the current lecture content") == true)
        assertTrue("Friendly offline message should be shown", offlineMsg?.text?.contains("SmartNotes AI is currently unable to answer") == true)
    }
}

