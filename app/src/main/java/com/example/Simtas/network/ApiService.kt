package com.example.Simtas.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

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
    val action: String = "send_message",
    val image_data: String? = null,
    val image_mime: String? = null
)

data class ChatHistoryRequest(
    val action: String = "get_history",
    val conversation_id: Int
)

data class ChatListConversationsRequest(
    val action: String = "list_conversations"
)

data class ChatDeleteConversationRequest(
    val action: String = "delete_conversation",
    val conversation_id: Int
)

data class ChatMessageContent(
    val id: Int? = null,
    val role: String = "user",
    val content: String = "",
    val provider: String? = null
)

data class ChatAttachmentItem(
    val id: Int,
    val file_name: String? = null,
    val file_size: Int? = null,
    val mime_type: String? = null,
    val url: String? = null,
    val created_at: String? = null
)

data class ChatHistoryMessageItem(
    val id: Int? = null,
    val role: String = "user",
    val message_content: String = "",
    val provider: String? = null,
    val created_at: String? = null,
    val attachments: List<ChatAttachmentItem> = emptyList()
)

data class ChatConversationSummary(
    val id: Int,
    val title: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ChatConversationItem(
    val id: Int,
    val title: String? = null,
    val conversation_title: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val message_count: Int? = null
) {
    val displayTitle: String
        get() = (title ?: conversation_title ?: "Conversation #$id").ifBlank { "Conversation #$id" }
}

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

data class ChatListConversationsResponse(
    val success: Boolean,
    val conversations: List<ChatConversationItem> = emptyList(),
    val error_code: String? = null,
    val message: String? = null
)

data class ChatDeleteConversationResponse(
    val success: Boolean,
    val message: String? = null,
    val deleted_conversation_id: Int? = null,
    val error_code: String? = null
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

    @Streaming
    @Headers("Accept: text/event-stream")
    @POST("ai/chat_stream.php")
    suspend fun streamMessage(
        @Body request: ChatSendMessageRequest
    ): Response<ResponseBody>

    @POST("ai/chat.php")
    suspend fun getHistory(
        @Body request: ChatHistoryRequest
    ): Response<ChatHistoryResponse>

    @POST("ai/chat.php")
    suspend fun listConversations(
        @Body request: ChatListConversationsRequest = ChatListConversationsRequest()
    ): Response<ChatListConversationsResponse>

    @POST("ai/chat.php")
    suspend fun deleteConversation(
        @Body request: ChatDeleteConversationRequest
    ): Response<ChatDeleteConversationResponse>

    @Streaming
    @GET("ai/get_chat_attachment.php")
    suspend fun getChatAttachment(
        @Query("id") id: Int
    ): Response<ResponseBody>
}
