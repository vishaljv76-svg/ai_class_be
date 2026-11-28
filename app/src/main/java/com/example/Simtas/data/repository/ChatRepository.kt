package com.example.Simtas.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.network.ChatConversationItem
import com.example.Simtas.network.ChatDeleteConversationRequest
import com.example.Simtas.network.ChatHistoryMessageItem
import com.example.Simtas.network.ChatHistoryRequest
import com.example.Simtas.network.ChatMessageContent
import com.example.Simtas.network.ChatSendMessageRequest
import com.example.Simtas.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject

sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Error(val message: String, val errorCode: String? = null, val isAuthError: Boolean = false) : ChatResult<Nothing>()
}

sealed class ChatStreamEvent {
    data class Start(
        val conversationId: Int,
        val conversationTitle: String,
        val userMessageId: Int? = null
    ) : ChatStreamEvent()

    data class Chunk(
        val chunkText: String,
        val index: Int = 0
    ) : ChatStreamEvent()

    data class Done(
        val conversationId: Int,
        val conversationTitle: String,
        val assistantMessageId: Int? = null,
        val totalLength: Int = 0
    ) : ChatStreamEvent()

    data class Error(
        val message: String,
        val errorCode: String? = null,
        val isAuthError: Boolean = false,
        val partialContent: String = ""
    ) : ChatStreamEvent()
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

    private val attachmentBitmapCache = java.util.concurrent.ConcurrentHashMap<Int, Bitmap>()

    fun getCachedAttachmentBitmap(attachmentId: Int): Bitmap? {
        return attachmentBitmapCache[attachmentId]
    }

    suspend fun getAttachmentBitmap(attachmentId: Int): Bitmap? = withContext(Dispatchers.IO) {
        attachmentBitmapCache[attachmentId]?.let { return@withContext it }
        try {
            val response = RetrofitClient.apiService.getChatAttachment(attachmentId)
            if (response.isSuccessful && response.body() != null) {
                val bytes = response.body()!!.bytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    attachmentBitmapCache[attachmentId] = bitmap
                    return@withContext bitmap
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun sendMessage(
        message: String,
        conversationId: Int? = null,
        subject: String? = null,
        lectureTitle: String? = null,
        imageData: String? = null,
        imageMime: String? = null
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
                lecture_title = lectureTitle,
                image_data = imageData,
                image_mime = imageMime
            )

            val response = RetrofitClient.apiService.sendMessage(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.conversation_id != null && body.assistant_message != null) {
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
                    502, 503 -> "AI chat service is temporarily busy. Offline fallback available."
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

    /**
     * Streams AI assistant responses in real time via Server-Sent Events (SSE).
     * Connects to POST ai/chat_stream.php using Retrofit @Streaming on Dispatchers.IO.
     * Emits Start, Chunk, Done, and Error events incrementally.
     */
    fun streamMessage(
        message: String,
        conversationId: Int? = null,
        subject: String? = null,
        lectureTitle: String? = null,
        imageData: String? = null,
        imageMime: String? = null
    ): Flow<ChatStreamEvent> = flow {
        if (!SessionManager.isLoggedIn()) {
            emit(
                ChatStreamEvent.Error(
                    message = "You are not logged in. Please log in to chat with SmartNotes AI.",
                    errorCode = "UNAUTHENTICATED",
                    isAuthError = true
                )
            )
            return@flow
        }

        val request = ChatSendMessageRequest(
            message = message.trim(),
            conversation_id = conversationId,
            subject = subject,
            lecture_title = lectureTitle,
            image_data = imageData,
            image_mime = imageMime
        )

        var responseBody: ResponseBody? = null
        var receivedDone = false
        var accumulatedText = ""
        var activeConvId: Int? = conversationId
        var activeConvTitle: String = ""

        try {
            val response = RetrofitClient.apiService.streamMessage(request)
            if (!response.isSuccessful || response.body() == null) {
                val code = response.code()
                val isAuth = code == 401 || code == 403
                if (isAuth) {
                    emit(
                        ChatStreamEvent.Error(
                            message = "Authentication expired or unauthorized. Please re-login.",
                            errorCode = "HTTP_$code",
                            isAuthError = true
                        )
                    )
                    return@flow
                }
                // Automatic non-streaming fallback for 404, server busy, or unavailable streaming endpoint
                executeNonStreamingFallback(
                    message = message,
                    conversationId = conversationId,
                    subject = subject,
                    lectureTitle = lectureTitle,
                    imageData = imageData,
                    imageMime = imageMime
                )
                return@flow
            }

            responseBody = response.body()!!
            val reader = responseBody.byteStream().bufferedReader(Charsets.UTF_8)

            var currentEvent = "message"
            val dataBuffer = StringBuilder()

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    // Empty line signals end of an SSE message block
                    if (dataBuffer.isNotEmpty()) {
                        val dataJson = dataBuffer.toString().trim()
                        dataBuffer.clear()

                        val eventToProcess = currentEvent
                        currentEvent = "message" // Reset event name to default for next block

                        if (dataJson.isNotEmpty()) {
                            try {
                                val json = JSONObject(dataJson)
                                when (eventToProcess) {
                                    "start" -> {
                                        val convId = json.optInt("conversation_id", -1)
                                        val title = json.optString("conversation_title", "")
                                        val userMsgId = if (json.has("user_message_id")) json.optInt("user_message_id") else null
                                        if (convId > 0) {
                                            activeConvId = convId
                                            activeConvTitle = title
                                            emit(
                                                ChatStreamEvent.Start(
                                                    conversationId = convId,
                                                    conversationTitle = title,
                                                    userMessageId = userMsgId
                                                )
                                            )
                                        }
                                    }
                                    "chunk" -> {
                                        val chunk = json.optString("chunk", "")
                                        val index = json.optInt("index", 0)
                                        if (chunk.isNotEmpty()) {
                                            accumulatedText += chunk
                                            emit(
                                                ChatStreamEvent.Chunk(
                                                    chunkText = chunk,
                                                    index = index
                                                )
                                            )
                                        }
                                    }
                                    "done" -> {
                                        receivedDone = true
                                        val convId = json.optInt("conversation_id", activeConvId ?: -1)
                                        val title = json.optString("conversation_title", activeConvTitle)
                                        val assistantMsgId = if (json.has("assistant_message_id")) json.optInt("assistant_message_id") else null
                                        val length = json.optInt("length", accumulatedText.length)
                                        emit(
                                            ChatStreamEvent.Done(
                                                conversationId = convId,
                                                conversationTitle = title,
                                                assistantMessageId = assistantMsgId,
                                                totalLength = length
                                            )
                                        )
                                    }
                                    "error" -> {
                                        val errCode = json.optString("error_code", "STREAM_ERROR")
                                        val errMsg = json.optString("message", "Streaming failed.")
                                        emit(
                                            ChatStreamEvent.Error(
                                                message = errMsg,
                                                errorCode = errCode,
                                                partialContent = accumulatedText
                                            )
                                        )
                                        return@flow
                                    }
                                }
                            } catch (_: Exception) {
                                // Non-fatal JSON parse error for malformed packet
                            }
                        }
                    }
                } else if (trimmed.startsWith("event:")) {
                    currentEvent = trimmed.removePrefix("event:").trim()
                } else if (trimmed.startsWith("data:")) {
                    val dataPart = trimmed.removePrefix("data:").trim()
                    if (dataBuffer.isNotEmpty()) {
                        dataBuffer.append("\n")
                    }
                    dataBuffer.append(dataPart)
                }
                line = reader.readLine()
            }

            // Process any trailing buffer if stream ended without double newline
            if (dataBuffer.isNotEmpty()) {
                val dataJson = dataBuffer.toString().trim()
                try {
                    val json = JSONObject(dataJson)
                    if (currentEvent == "done") {
                        receivedDone = true
                        val convId = json.optInt("conversation_id", activeConvId ?: -1)
                        val title = json.optString("conversation_title", activeConvTitle)
                        val assistantMsgId = if (json.has("assistant_message_id")) json.optInt("assistant_message_id") else null
                        val length = json.optInt("length", accumulatedText.length)
                        emit(
                            ChatStreamEvent.Done(
                                conversationId = convId,
                                conversationTitle = title,
                                assistantMessageId = assistantMsgId,
                                totalLength = length
                            )
                        )
                    } else if (currentEvent == "error") {
                        val errCode = json.optString("error_code", "STREAM_ERROR")
                        val errMsg = json.optString("message", "Streaming failed.")
                        emit(
                            ChatStreamEvent.Error(
                                message = errMsg,
                                errorCode = errCode,
                                partialContent = accumulatedText
                            )
                        )
                        return@flow
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }

            // Stalled, empty or abruptly terminated stream without a valid Done event
            if (!receivedDone) {
                if (accumulatedText.isEmpty()) {
                    // No chunks received: safe to fall back to non-streaming endpoint
                    executeNonStreamingFallback(
                        message = message,
                        conversationId = conversationId,
                        subject = subject,
                        lectureTitle = lectureTitle,
                        imageData = imageData,
                        imageMime = imageMime
                    )
                } else {
                    // Partial chunks already displayed: avoid duplicate requests
                    emit(
                        ChatStreamEvent.Error(
                            message = "Stream terminated unexpectedly before completion.",
                            errorCode = "STREAM_INCOMPLETE",
                            partialContent = accumulatedText
                        )
                    )
                }
            }
        } catch (e: Exception) {
            if (accumulatedText.isEmpty()) {
                // Failure occurred before receiving any chunks: trigger non-streaming fallback
                executeNonStreamingFallback(
                    message = message,
                    conversationId = conversationId,
                    subject = subject,
                    lectureTitle = lectureTitle,
                    imageData = imageData,
                    imageMime = imageMime
                )
            } else {
                // Response already started: do not duplicate requests
                emit(
                    ChatStreamEvent.Error(
                        message = e.localizedMessage ?: "Network connection failed while streaming response.",
                        errorCode = "NETWORK_EXCEPTION",
                        partialContent = accumulatedText
                    )
                )
            }
        } finally {
            try {
                responseBody?.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Automatic non-streaming fallback helper: calls POST ai/chat.php when streaming fails
     * before chunks begin, emitting Start, Chunk, and Done events so the UI receives the full answer.
     */
    private suspend fun FlowCollector<ChatStreamEvent>.executeNonStreamingFallback(
        message: String,
        conversationId: Int?,
        subject: String?,
        lectureTitle: String?,
        imageData: String?,
        imageMime: String?
    ) {
        val fallbackResult = sendMessage(
            message = message,
            conversationId = conversationId,
            subject = subject,
            lectureTitle = lectureTitle,
            imageData = imageData,
            imageMime = imageMime
        )
        when (fallbackResult) {
            is ChatResult.Success -> {
                val data = fallbackResult.data
                val text = data.assistantMessage?.content ?: data.message
                emit(
                    ChatStreamEvent.Start(
                        conversationId = data.conversationId,
                        conversationTitle = data.conversationTitle,
                        userMessageId = data.userMessage.id
                    )
                )
                if (text.isNotEmpty()) {
                    emit(
                        ChatStreamEvent.Chunk(
                            chunkText = text,
                            index = 1
                        )
                    )
                }
                emit(
                    ChatStreamEvent.Done(
                        conversationId = data.conversationId,
                        conversationTitle = data.conversationTitle,
                        assistantMessageId = data.assistantMessage?.id,
                        totalLength = text.length
                    )
                )
            }
            is ChatResult.Error -> {
                emit(
                    ChatStreamEvent.Error(
                        message = fallbackResult.message,
                        errorCode = fallbackResult.errorCode,
                        isAuthError = fallbackResult.isAuthError
                    )
                )
            }
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

    suspend fun deleteConversation(conversationId: Int): ChatResult<Boolean> = withContext(Dispatchers.IO) {
        if (!SessionManager.isLoggedIn()) {
            return@withContext ChatResult.Error(
                message = "You are not logged in.",
                errorCode = "UNAUTHENTICATED",
                isAuthError = true
            )
        }

        try {
            val request = ChatDeleteConversationRequest(conversation_id = conversationId)
            val response = RetrofitClient.apiService.deleteConversation(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    ChatResult.Success(true)
                } else {
                    ChatResult.Error(
                        message = body.message ?: "Failed to delete conversation",
                        errorCode = body.error_code
                    )
                }
            } else {
                val code = response.code()
                ChatResult.Error(
                    message = "Failed to delete conversation ($code)",
                    errorCode = "HTTP_$code",
                    isAuthError = code == 401 || code == 403
                )
            }
        } catch (e: Exception) {
            ChatResult.Error(
                message = e.localizedMessage ?: "Failed to delete conversation",
                errorCode = "NETWORK_EXCEPTION"
            )
        }
    }
}
