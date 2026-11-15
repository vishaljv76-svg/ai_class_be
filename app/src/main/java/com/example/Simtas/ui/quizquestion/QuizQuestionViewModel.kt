package com.example.Simtas.ui.quizquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.QuizRepository
import com.example.Simtas.data.repository.RealQuizQuestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for [QuizQuestionScreen] adhering to MVVM architecture.
 * Loads dynamic quiz questions generated directly from the student's saved notes.
 */
class QuizQuestionViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizQuestionUiState())
    val uiState: StateFlow<QuizQuestionUiState> = _uiState.asStateFlow()

    private var questionsList: List<RealQuizQuestion> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, String>()

    private fun isAndroidRuntime(): Boolean {
        return try {
            android.os.Looper.getMainLooper() != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun safeScope(): CoroutineScope {
        return externalScope ?: if (isAndroidRuntime()) {
            viewModelScope
        } else {
            CoroutineScope(Dispatchers.IO)
        }
    }

    init {
        loadQuiz()
    }

    fun loadQuiz() {
        if (!isAndroidRuntime()) {
            runBlocking {
                loadQuizInternal()
            }
        } else {
            safeScope().launch(Dispatchers.IO) {
                loadQuizInternal()
            }
        }
    }

    private suspend fun loadQuizInternal() {
        val result = QuizRepository.getQuiz()
        if (result.isSuccess && result.questions.isNotEmpty()) {
            questionsList = result.questions
            displayQuestion(0)
        }
    }

    private fun displayQuestion(index: Int) {
        val q = questionsList.getOrNull(index) ?: return
        val currentQNum = index + 1
        val selectedOpt = selectedAnswers[currentQNum]

        _uiState.update {
            it.copy(
                currentQuestionIndex = currentQNum,
                totalQuestions = questionsList.size,
                modeText = q.modeText,
                topicText = q.topicText,
                questionText = q.questionText,
                instructionText = q.instructionText,
                options = q.options,
                selectedOptionKey = selectedOpt,
                statusNoteText = if (selectedOpt != null) "You've selected $selectedOpt · You can change your answer" else "Select an option to proceed",
                isQuizFinished = false
            )
        }
    }

    fun selectOption(optionKey: String) {
        val currentQNum = _uiState.value.currentQuestionIndex
        selectedAnswers[currentQNum] = optionKey
        _uiState.update {
            it.copy(
                selectedOptionKey = optionKey,
                statusNoteText = "You've selected $optionKey · You can change your answer"
            )
        }
    }

    fun onNextClicked() {
        val current = _uiState.value.currentQuestionIndex
        val total = _uiState.value.totalQuestions
        if (current < total) {
            displayQuestion(current)
        } else {
            val submitAction: suspend () -> Unit = {
                val activeQuizId = QuizRepository.cachedActiveQuiz?.quizId ?: 0
                if (activeQuizId > 0) {
                    QuizRepository.submitQuiz(activeQuizId, selectedAnswers)
                }
            }
            if (!isAndroidRuntime()) {
                runBlocking { submitAction() }
            } else {
                safeScope().launch(Dispatchers.IO) { submitAction() }
            }
            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }
}
