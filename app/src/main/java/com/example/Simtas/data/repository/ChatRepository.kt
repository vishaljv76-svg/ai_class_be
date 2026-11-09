package com.example.Simtas.data.repository

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.network.ChatConversationItem
import com.example.Simtas.network.ChatHistoryMessageItem
import com.example.Simtas.network.ChatHistoryRequest
import com.example.Simtas.network.ChatMessageContent
import com.example.Simtas.network.ChatSendMessageRequest
import com.example.Simtas.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Error(val message: String, val errorCode: String? = null, val isAuthError: Boolean = false) : ChatResult<Nothing>()
}

data class ChatSendResultData(
    val conversationId: Int,
    val conversationTitle: String,
    val userMessage: ChatMessageContent,
    val assistantMessage: ChatMessageContent?,
    val aiStatus: String,
    val message: String
)

data class ChatHistoryResultData(
    val conversationId: Int,
    val title: String,
    val messages: List<ChatHistoryMessageItem>
)

object ChatRepository {

    suspend fun sendMessage(
        message: String,
        conversationId: Int? = null,
        subject: String? = null,
        lectureTitle: String? = null
    ): ChatResult<ChatSendResultData> = withContext(Dispatchers.IO) {
        if (!SessionManager.isLoggedIn()) {
            return@withContext ChatResult.Error(
                message = "You are not logged in. Please log in to chat with SmartNotes AI.",
                errorCode = "UNAUTHENTICATED",
                isAuthError = true
            )
        }

        try {
            val request = ChatSendMessageRequest(
                message = message.trim(),
                conversation_id = conversationId,
                subject = subject,
                lecture_title = lectureTitle
            )

            val response = RetrofitClient.apiService.sendMessage(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.conversation_id != null) {
                    ChatResult.Success(
                        ChatSendResultData(
                            conversationId = body.conversation_id,
                            conversationTitle = body.conversation_title ?: "New Conversation",
                            userMessage = body.user_message ?: ChatMessageContent(role = "user", content = message),
                            assistantMessage = body.assistant_message,
                            aiStatus = body.ai_status ?: "completed",
                            message = body.message ?: "Success"
                        )
                    )
                } else {
                    ChatResult.Error(
                        message = body.message ?: "Failed to generate AI response",
                        errorCode = body.error_code
                    )
                }
            } else {
                val code = response.code()
                val isAuth = code == 401 || code == 403
                val errorMsg = when (code) {
                    401, 403 -> "Authentication expired or unauthorized. Please re-login."
                    404 -> "Chat service endpoint not found."
                    429 -> "Rate limit reached. Please wait a moment before sending more messages."
                    503 -> "AI chat service is temporarily unavailable. Please try again shortly."
                    else -> "Server error ($code). Please try again later."
                }
                ChatResult.Error(
                    message = errorMsg,
                    errorCode = "HTTP_$code",
                    isAuthError = isAuth
                )
            }
        } catch (e: Exception) {
            ChatResult.Error(
                message = e.localizedMessage ?: "Network connection failed. Please check your internet connection.",
                errorCode = "NETWORK_EXCEPTION"
            )
        }
    }

    suspend fun getHistory(conversationId: Int): ChatResult<ChatHistoryResultData> = withContext(Dispatchers.IO) {
        if (!SessionManager.isLoggedIn()) {
            return@withContext ChatResult.Error(
                message = "You are not logged in. Please log in to view conversation history.",
                errorCode = "UNAUTHENTICATED",
                isAuthError = true
            )
        }

        try {
            val request = ChatHistoryRequest(conversation_id = conversationId)
            val response = RetrofitClient.apiService.getHistory(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.conversation != null) {
                    ChatResult.Success(
                        ChatHistoryResultData(
                            conversationId = body.conversation.id,
                            title = body.conversation.title,
                            messages = body.messages
                        )
                    )
                } else {
                    ChatResult.Error(
                        message = body.message ?: "Failed to load chat history",
                        errorCode = body.error_code
                    )
                }
            } else {
                val code = response.code()
                ChatResult.Error(
                    message = "Failed to retrieve history ($code)",
                    errorCode = "HTTP_$code",
                    isAuthError = code == 401 || code == 403
                )
            }
        } catch (e: Exception) {
            ChatResult.Error(
                message = e.localizedMessage ?: "Network connection failed.",
                errorCode = "NETWORK_EXCEPTION"
            )
        }
    }

    suspend fun listConversations(): ChatResult<List<ChatConversationItem>> = withContext(Dispatchers.IO) {
        if (!SessionManager.isLoggedIn()) {
            return@withContext ChatResult.Error(
                message = "You are not logged in. Please log in to view previous conversations.",
                errorCode = "UNAUTHENTICATED",
                isAuthError = true
            )
        }

        try {
            val response = RetrofitClient.apiService.listConversations()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    ChatResult.Success(body.conversations)
                } else {
                    ChatResult.Error(
                        message = body.message ?: "Failed to load conversations",
                        errorCode = body.error_code
                    )
                }
            } else {
                ChatResult.Error(
                    message = "Server error (${response.code()})",
                    errorCode = "HTTP_${response.code()}"
                )
            }
        } catch (e: Exception) {
            ChatResult.Error(
                message = e.localizedMessage ?: "Failed to fetch conversations",
                errorCode = "NETWORK_EXCEPTION"
            )
        }
    }
}
