package com.example.Simtas.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Simtas.data.repository.NotesRepositoryImpl
import com.example.Simtas.data.repository.QuizRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for [QuizGeneratorScreen] adhering to MVVM architecture.
 * Connects AI Quiz directly to the student's saved My Notes.
 */
class QuizGeneratorViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizGeneratorUiState())
    val uiState: StateFlow<QuizGeneratorUiState> = _uiState.asStateFlow()

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
        loadSavedNotes()
    }

    fun loadSavedNotes() {
        if (!isAndroidRuntime()) {
            runBlocking {
                loadSavedNotesInternal()
            }
        } else {
            safeScope().launch(Dispatchers.IO) {
                loadSavedNotesInternal()
            }
        }
    }

    private suspend fun loadSavedNotesInternal() {
        val result = NotesRepositoryImpl.fetchNotes()
        val notes = result.notes
        if (notes.isEmpty()) {
            _uiState.update {
                it.copy(
                    savedNotes = emptyList(),
                    isMyNotesEmpty = true,
                    lectureTitle = "No notes saved yet",
                    lectureMetadata = "Ask questions in AI Copilot or record a lecture first",
                    subjectOptions = listOf("No notes available"),
                    selectedSubject = "No notes available",
                    selectedNoteIndex = 0,
                    selectedNoteId = null,
                    selectedNoteKey = null
                )
            }
        } else {
            val latest = notes.first()
            val subjectList = mutableListOf("All Saved My Notes (${notes.size} notes)")
            subjectList.addAll(notes.map { "${it.title} · ${it.subject}" })

            _uiState.update {
                it.copy(
                    savedNotes = notes,
                    isMyNotesEmpty = false,
                    lectureTitle = "All My Notes (${notes.size} notes available)",
                    lectureMetadata = "Latest: ${latest.title} · ${latest.dateText}",
                    subjectOptions = subjectList,
                    selectedSubject = subjectList.first(),
                    selectedNoteIndex = 0,
                    selectedNoteId = null,
                    selectedNoteKey = null,
                    errorMessage = null
                )
            }
        }
    }

    fun onSubjectSelected(subject: String) {
        val notes = _uiState.value.savedNotes
        val idx = _uiState.value.subjectOptions.indexOf(subject)
        if (idx <= 0 || notes.isEmpty()) {
            // "All Saved My Notes"
            _uiState.update {
                it.copy(
                    selectedSubject = subject,
                    selectedNoteIndex = 0,
                    selectedNoteId = null,
                    selectedNoteKey = null,
                    lectureTitle = if (notes.isNotEmpty()) "All My Notes (${notes.size} notes)" else "No notes saved yet",
                    lectureMetadata = if (notes.isNotEmpty()) "Comprehensive quiz across all your saved notes" else "Ask questions in AI Study Copilot first"
                )
            }
        } else {
            // Specific note selected
            val note = notes.getOrNull(idx - 1)
            if (note != null) {
                val parsedId = note.id.toIntOrNull()
                _uiState.update {
                    it.copy(
                        selectedSubject = subject,
                        selectedNoteIndex = idx,
                        selectedNoteId = parsedId,
                        selectedNoteKey = note.id,
                        lectureTitle = note.title,
                        lectureMetadata = "${note.subject} · ${note.dateText} · ${note.durationText}"
                    )
                }
            }
        }
    }

    fun onTopicInputChanged(topic: String) {
        _uiState.update {
            it.copy(
                topicInput = topic,
                errorMessage = null
            )
        }
    }

    fun onNumberOfQuestionsSelected(count: Int) {
        _uiState.update {
            it.copy(
                numberOfQuestions = count,
                estimatedTimeText = "$count multiple-choice questions · About $count min"
            )
        }
    }

    fun onQuestionTypeSelected(type: QuestionType) {
        _uiState.update { it.copy(questionType = type) }
    }

    fun onDifficultySelected(difficulty: DifficultyLevel) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onGenerateQuizClicked(onQuizReady: () -> Unit = {}) {
        val topic = _uiState.value.topicInput.trim()
        val isTopicMode = topic.isNotBlank()

        if (!isTopicMode && _uiState.value.isMyNotesEmpty) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val executeGeneration: suspend () -> Unit = {
            val noteId = if (isTopicMode) null else _uiState.value.selectedNoteId
            val noteKey = if (isTopicMode) null else _uiState.value.selectedNoteKey
            val numQ = _uiState.value.numberOfQuestions
            val diff = when (_uiState.value.difficulty) {
                DifficultyLevel.EASY -> "Easy"
                DifficultyLevel.MODERATE -> "Medium"
                DifficultyLevel.HARD -> "Hard"
                DifficultyLevel.MIXED -> "Mixed"
            }
            val qType = when (_uiState.value.questionType) {
                QuestionType.MCQ -> "MCQ"
                QuestionType.TRUE_FALSE -> "True / False"
                QuestionType.MIXED -> "Mixed"
            }

            val result = QuizRepository.generateQuiz(
                topic = if (isTopicMode) topic else null,
                noteId = noteId,
                noteKey = noteKey,
                numberOfQuestions = numQ,
                difficulty = diff,
                questionType = qType
            )

            _uiState.update { it.copy(isLoading = false) }

            if (result.isSuccess && result.questions.isNotEmpty()) {
                onQuizReady()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.message ?: "Failed to generate quiz questions"
                    )
                }
            }
        }

        if (!isAndroidRuntime()) {
            runBlocking { executeGeneration() }
        } else {
            safeScope().launch(Dispatchers.IO) { executeGeneration() }
        }
    }
}
