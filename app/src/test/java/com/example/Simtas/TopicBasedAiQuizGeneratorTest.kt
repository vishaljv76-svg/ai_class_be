package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.QuizDetailResult
import com.example.Simtas.data.repository.QuizRepository
import com.example.Simtas.data.repository.QuizSubmissionResult
import com.example.Simtas.data.repository.RealQuizQuestion
import com.example.Simtas.ui.quiz.DifficultyLevel
import com.example.Simtas.ui.quiz.QuestionType
import com.example.Simtas.ui.quiz.QuizGeneratorUiState
import com.example.Simtas.ui.quiz.QuizGeneratorViewModel
import com.example.Simtas.ui.quizquestion.QuizOptionItem
import com.example.Simtas.ui.quizquestion.QuizQuestionViewModel
import com.example.Simtas.ui.quizresults.AnswerReviewItem
import com.example.Simtas.ui.quizresults.QuizResultsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit & Integration Tests for:
 * Topic-Based AI Quiz Generator (SmartNotes AI Safe Feature Addition).
 */
class TopicBasedAiQuizGeneratorTest {

    @Before
    fun setUp() {
        SessionManager.clearSession()
        QuizRepository.cachedActiveQuiz = null
        QuizRepository.cachedSubmissionResult = null
    }

    @After
    fun tearDown() {
        SessionManager.clearSession()
        QuizRepository.cachedActiveQuiz = null
        QuizRepository.cachedSubmissionResult = null
    }

    @Test
    fun test1_topicInputAndSuggestionChips_updateUiStateProperly() {
        val viewModel = QuizGeneratorViewModel()
        assertEquals("", viewModel.uiState.value.topicInput)

        // Type a custom topic
        viewModel.onTopicInputChanged("NPTEL Exam – Computer Networks")
        assertEquals("NPTEL Exam – Computer Networks", viewModel.uiState.value.topicInput)

        // Select a suggestion chip
        viewModel.onTopicInputChanged("Data Structures")
        assertEquals("Data Structures", viewModel.uiState.value.topicInput)

        viewModel.onTopicInputChanged("Database Management Systems")
        assertEquals("Database Management Systems", viewModel.uiState.value.topicInput)
    }

    @Test
    fun test2_optionsConfiguration_supportsRequiredValues() {
        val viewModel = QuizGeneratorViewModel()

        // Question Counts: 10, 20, 30, 50
        listOf(10, 20, 30, 50).forEach { count ->
            viewModel.onNumberOfQuestionsSelected(count)
            assertEquals(count, viewModel.uiState.value.numberOfQuestions)
            assertTrue(viewModel.uiState.value.estimatedTimeText.contains("$count min"))
        }

        // Difficulty: Easy, Medium, Hard, Mixed
        listOf(
            DifficultyLevel.EASY,
            DifficultyLevel.MODERATE,
            DifficultyLevel.HARD,
            DifficultyLevel.MIXED
        ).forEach { diff ->
            viewModel.onDifficultySelected(diff)
            assertEquals(diff, viewModel.uiState.value.difficulty)
        }

        // Question Formats: MCQ Only, Mixed
        viewModel.onQuestionTypeSelected(QuestionType.MCQ)
        assertEquals(QuestionType.MCQ, viewModel.uiState.value.questionType)

        viewModel.onQuestionTypeSelected(QuestionType.MIXED)
        assertEquals(QuestionType.MIXED, viewModel.uiState.value.questionType)
    }

    @Test
    fun test3_topicQuizGeneration_allowedEvenWhenMyNotesIsEmpty() = runBlocking {
        // User with zero saved notes
        SessionManager.saveSession(null, "token_301", 301, "Test Student", "student@campus.edu")

        val viewModel = QuizGeneratorViewModel()
        viewModel.loadSavedNotes()
        assertTrue("My Notes should be empty", viewModel.uiState.value.isMyNotesEmpty)

        // Without topic, generation is blocked
        var callbackCalled = false
        viewModel.onGenerateQuizClicked { callbackCalled = true }
        assertFalse("Cannot generate without topic when notes are empty", callbackCalled)

        // With typed topic, topic-based generation is active!
        viewModel.onTopicInputChanged("Operating Systems")
        assertEquals("Operating Systems", viewModel.uiState.value.topicInput)
    }

    @Test
    fun test4_quizTaking_supportsPreviousNextAndAnswerSelection() {
        // Prepare mock active quiz with 3 questions
        val mockQuestions = listOf(
            RealQuizQuestion(
                id = 1,
                questionNumber = 1,
                questionText = "Which layer of OSI model is responsible for routing?",
                topic = "Computer Networks",
                topicText = "Computer Networks · Moderate",
                instructionText = "Choose one answer.",
                modeText = "Moderate · MCQ",
                options = listOf(
                    QuizOptionItem("A", "Physical Layer"),
                    QuizOptionItem("B", "Data Link Layer"),
                    QuizOptionItem("C", "Network Layer"),
                    QuizOptionItem("D", "Transport Layer")
                ),
                correctOption = "C",
                explanation = "Network layer handles routing and logical addressing."
            ),
            RealQuizQuestion(
                id = 2,
                questionNumber = 2,
                questionText = "TCP is a connectionless protocol.",
                topic = "Transport Layer",
                topicText = "Transport Layer · Moderate",
                instructionText = "Choose True or False.",
                modeText = "Moderate · True / False",
                options = listOf(
                    QuizOptionItem("A", "True"),
                    QuizOptionItem("B", "False")
                ),
                correctOption = "B",
                explanation = "TCP is connection-oriented; UDP is connectionless."
            ),
            RealQuizQuestion(
                id = 3,
                questionNumber = 3,
                questionText = "What is the default port for HTTP?",
                topic = "Application Layer",
                topicText = "Application Layer · Easy",
                instructionText = "Choose one answer.",
                modeText = "Easy · MCQ",
                options = listOf(
                    QuizOptionItem("A", "80"),
                    QuizOptionItem("B", "443"),
                    QuizOptionItem("C", "21"),
                    QuizOptionItem("D", "22")
                ),
                correctOption = "A",
                explanation = "Port 80 is the standard unencrypted HTTP port."
            )
        )

        QuizRepository.cachedActiveQuiz = QuizDetailResult(
            isSuccess = true,
            quizId = 777,
            title = "Quiz: Computer Networks",
            subject = "Computer Networks",
            totalQuestions = 3,
            questions = mockQuestions
        )

        val qViewModel = QuizQuestionViewModel()
        qViewModel.loadQuiz()

        // 1. Initial State: Question 1
        val state1 = qViewModel.uiState.value
        assertEquals(1, state1.currentQuestionIndex)
        assertEquals(3, state1.totalQuestions)
        assertEquals("Which layer of OSI model is responsible for routing?", state1.questionText)
        assertEquals(4, state1.options.size)
        assertNull(state1.selectedOptionKey)

        // 2. Select Option C
        qViewModel.selectOption("C")
        assertEquals("C", qViewModel.uiState.value.selectedOptionKey)

        // 3. Move to Question 2 via onNextClicked()
        qViewModel.onNextClicked()
        val state2 = qViewModel.uiState.value
        assertEquals(2, state2.currentQuestionIndex)
        assertEquals("TCP is a connectionless protocol.", state2.questionText)
        assertEquals(2, state2.options.size) // True/False has 2 options

        // 4. Select Option B for Question 2
        qViewModel.selectOption("B")
        assertEquals("B", qViewModel.uiState.value.selectedOptionKey)

        // 5. Navigate backwards via onPreviousClicked()
        qViewModel.onPreviousClicked()
        val statePrevious = qViewModel.uiState.value
        assertEquals(1, statePrevious.currentQuestionIndex)
        assertEquals("Which layer of OSI model is responsible for routing?", statePrevious.questionText)
        assertEquals("C", statePrevious.selectedOptionKey) // Answer retained!

        // 6. Navigate forward to Question 2 and Question 3
        qViewModel.onNextClicked()
        assertEquals(2, qViewModel.uiState.value.currentQuestionIndex)
        assertEquals("B", qViewModel.uiState.value.selectedOptionKey) // Answer retained!

        qViewModel.onNextClicked()
        assertEquals(3, qViewModel.uiState.value.currentQuestionIndex)
        assertEquals("What is the default port for HTTP?", qViewModel.uiState.value.questionText)

        // 7. Select Option A on Question 3
        qViewModel.selectOption("A")
        assertEquals("A", qViewModel.uiState.value.selectedOptionKey)

        // 8. Submit Quiz on last question
        var finishCalled = false
        qViewModel.onNextClicked(onQuizFinish = { finishCalled = true })
        assertTrue("Quiz finished callback must be triggered on final question", finishCalled)
        assertTrue("isQuizFinished must be true", qViewModel.uiState.value.isQuizFinished)
    }

    @Test
    fun test5_quizResults_populatesScoreAndExplanationsAccurately() {
        val reviewItems = listOf(
            AnswerReviewItem(
                questionNumber = "Q1",
                questionTopic = "Routing",
                isCorrect = true,
                userAnswerText = "C — Network Layer",
                correctAnswerText = "C — Network Layer",
                explanationText = "Network layer handles routing."
            ),
            AnswerReviewItem(
                questionNumber = "Q2",
                questionTopic = "Transport Protocols",
                isCorrect = true,
                userAnswerText = "B — False",
                correctAnswerText = "B — False",
                explanationText = "TCP is connection-oriented."
            ),
            AnswerReviewItem(
                questionNumber = "Q3",
                questionTopic = "Ports",
                isCorrect = false,
                userAnswerText = "B — 443",
                correctAnswerText = "A — 80",
                explanationText = "Port 80 is HTTP; 443 is HTTPS."
            )
        )

        QuizRepository.cachedActiveQuiz = QuizDetailResult(
            isSuccess = true,
            quizId = 888,
            title = "Quiz: NPTEL Exam – Computer Networks",
            subject = "Computer Networks",
            totalQuestions = 3
        )

        QuizRepository.cachedSubmissionResult = QuizSubmissionResult(
            isSuccess = true,
            scorePercentage = 67,
            correctCount = 2,
            totalQuestions = 3,
            feedbackHeader = "Good understanding",
            feedbackText = "You understand the primary mechanisms well.",
            reviewItems = reviewItems
        )

        val resultsViewModel = QuizResultsViewModel()
        val rState = resultsViewModel.uiState.value

        assertEquals(67, rState.scorePercentage)
        assertEquals(2, rState.correctCount)
        assertEquals(3, rState.totalQuestions)
        assertEquals("Solid progress on this topic.", rState.titleMessage)
        assertEquals("Good understanding", rState.feedbackHeader)
        assertEquals(3, rState.reviewItems.size)
        assertTrue(rState.reviewItems[0].isCorrect)
        assertFalse(rState.reviewItems[2].isCorrect)
        assertEquals("Port 80 is HTTP; 443 is HTTPS.", rState.reviewItems[2].explanationText)

        // Test retake quiz
        resultsViewModel.onRetryQuizClicked()
        assertNull("Cached submission should be reset on retry", QuizRepository.cachedSubmissionResult)
    }
}
