package com.example.Simtas.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- Existing Authentication Models ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserData?
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val initials: String
)

// --- SmartNotes AI Chat Models (ai/chat.php) ---
data class ChatSendMessageRequest(
    val message: String,
    val conversation_id: Int? = null,
    val subject: String? = null,
    val lecture_title: String? = null,
    val action: String = "send_message"
)

data class ChatHistoryRequest(
    val action: String = "get_history",
    val conversation_id: Int
)

data class ChatMessageContent(
    val id: Int? = null,
    val role: String = "user",
    val content: String = "",
    val provider: String? = null
)

data class ChatHistoryMessageItem(
    val id: Int? = null,
    val role: String = "user",
    val message_content: String = "",
    val provider: String? = null,
    val created_at: String? = null
)

data class ChatConversationSummary(
    val id: Int,
    val title: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ChatSendResponse(
    val success: Boolean,
    val conversation_id: Int? = null,
    val conversation_title: String? = null,
    val user_message: ChatMessageContent? = null,
    val assistant_message: ChatMessageContent? = null,
    val ai_status: String? = null,
    val message: String? = null,
    val error_code: String? = null
)

data class ChatHistoryResponse(
    val success: Boolean,
    val conversation: ChatConversationSummary? = null,
    val messages: List<ChatHistoryMessageItem> = emptyList(),
    val error_code: String? = null,
    val message: String? = null
)

interface ApiService {

    // Existing Login Endpoint
    @POST("auth/login.php")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // SmartNotes AI Live Chat Endpoints
    @POST("ai/chat.php")
    suspend fun sendMessage(
        @Body request: ChatSendMessageRequest
    ): Response<ChatSendResponse>

    @POST("ai/chat.php")
    suspend fun getHistory(
        @Body request: ChatHistoryRequest
    ): Response<ChatHistoryResponse>
}
