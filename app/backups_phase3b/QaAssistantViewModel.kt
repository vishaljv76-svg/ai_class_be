package com.example.Simtas.ui.qa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.ml.QuestionAnsweringEngine
import com.example.Simtas.data.repository.ChatRepository
import com.example.Simtas.data.repository.ChatResult
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MessageSource {
    USER,
    ONLINE_GEMINI,
    OFFLINE_ENGINE
}

data class QaMessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSource,
    val text: String,
    val explanation: String = "",
    val provider: String? = null,
    val isAnswered: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class QaAssistantUiState(
    val messages: List<QaMessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val conversationId: Int? = null,
    val conversationTitle: String? = null,
    val errorMessage: String? = null,
    val isOfflineModeActive: Boolean = false
)

class QaAssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QaAssistantUiState())
    val uiState: StateFlow<QaAssistantUiState> = _uiState.asStateFlow()

    fun askQuestion(
        question: String,
        subject: String? = null,
        lectureTitle: String? = null,
        transcriptText: String = "",
        detailedNotes: DetailedNotesUiState? = null,
        summaryText: String = "",
        keyTakeaways: List<String> = emptyList(),
        forceOffline: Boolean = false
    ) {
        val q = question.trim()
        if (q.isBlank()) return
        if (_uiState.value.isLoading) return // Prevent duplicate requests while loading

        // Add user message to UI immediately
        val userMsg = QaMessageItem(
            sender = MessageSource.USER,
            text = q
        )
        _uiState.update {
            it.copy(
                messages = listOf(userMsg) + it.messages,
                isLoading = true,
                errorMessage = null
            )
        }

        if (forceOffline) {
            executeOfflineAnswer(q, transcriptText, detailedNotes, summaryText, keyTakeaways)
            return
        }

        viewModelScope.launch {
            val currentConvId = _uiState.value.conversationId
            val result = ChatRepository.sendMessage(
                message = q,
                conversationId = currentConvId,
                subject = subject,
                lectureTitle = lectureTitle
            )

            when (result) {
                is ChatResult.Success -> {
                    val data = result.data
                    val assistantText = data.assistantMessage?.content ?: data.message
                    val provider = data.assistantMessage?.provider ?: "gemini"

                    val assistantMsg = QaMessageItem(
                        sender = MessageSource.ONLINE_GEMINI,
                        text = assistantText,
                        provider = provider,
                        isAnswered = true
                    )

                    _uiState.update {
                        it.copy(
                            messages = listOf(assistantMsg) + it.messages,
                            conversationId = data.conversationId,
                            conversationTitle = data.conversationTitle,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is ChatResult.Error -> {
                    // Graceful offline fallback with clear labelling
                    val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
                        question = q,
                        transcriptText = transcriptText,
                        detailedNotes = detailedNotes,
                        summaryText = summaryText,
                        keyTakeaways = keyTakeaways
                    )

                    val offlineMsg = QaMessageItem(
                        sender = MessageSource.OFFLINE_ENGINE,
                        text = offlineAnswer.answer,
                        explanation = if (offlineAnswer.explanation.isNotBlank()) {
                            "${offlineAnswer.explanation} (Note: Offline Fallback due to ${result.message})"
                        } else {
                            "Offline Fallback (Live Gemini unavailable: ${result.message})"
                        },
                        provider = "Offline Curriculum Knowledge",
                        isAnswered = offlineAnswer.isAnswered
                    )

                    _uiState.update {
                        it.copy(
                            messages = listOf(offlineMsg) + it.messages,
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun executeOfflineAnswer(
        question: String,
        transcriptText: String,
        detailedNotes: DetailedNotesUiState?,
        summaryText: String,
        keyTakeaways: List<String>
    ) {
        val offlineAnswer = QuestionAnsweringEngine.answerQuestion(
            question = question,
            transcriptText = transcriptText,
            detailedNotes = detailedNotes,
            summaryText = summaryText,
            keyTakeaways = keyTakeaways
        )

        val offlineMsg = QaMessageItem(
            sender = MessageSource.OFFLINE_ENGINE,
            text = offlineAnswer.answer,
            explanation = offlineAnswer.explanation,
            provider = "Offline Curriculum Knowledge",
            isAnswered = offlineAnswer.isAnswered
        )

        _uiState.update {
            it.copy(
                messages = listOf(offlineMsg) + it.messages,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun loadConversationHistory(conversationId: Int) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = ChatRepository.getHistory(conversationId)
            when (result) {
                is ChatResult.Success -> {
                    val historyItems = result.data.messages.map { msg ->
                        val isUser = msg.role.equals("user", ignoreCase = true)
                        QaMessageItem(
                            sender = if (isUser) MessageSource.USER else MessageSource.ONLINE_GEMINI,
                            text = msg.message_content,
                            provider = msg.provider ?: if (isUser) null else "gemini"
                        )
                    }.reversed()

                    _uiState.update {
                        it.copy(
                            messages = historyItems,
                            conversationId = result.data.conversationId,
                            conversationTitle = result.data.title,
                            isLoading = false
                        )
                    }
                }
                is ChatResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startNewConversation() {
        _uiState.update {
            QaAssistantUiState()
        }
    }
}

