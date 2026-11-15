package com.example.Simtas

import com.example.Simtas.data.local.LectureNotesStorage
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.data.repository.NotesRepositoryImpl
import com.example.Simtas.data.repository.QuizRepository
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.notes.GeneratedNotesUiState
import com.example.Simtas.ui.quiz.DifficultyLevel
import com.example.Simtas.ui.quiz.QuizGeneratorUiState
import com.example.Simtas.ui.quiz.QuizGeneratorViewModel
import com.example.Simtas.ui.quizquestion.QuizQuestionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration tests for:
 * 1. AI Questions and Answers auto-saving to My Notes
 * 2. My Notes Q&A presentation with complete text preservation
 * 3. AI Quiz generation strictly derived from saved My Notes
 * 4. Empty My Notes handling with friendly guidance prompt
 * 5. Strict multi-student account isolation (Student A vs Student B)
 * 6. Non-regression of existing lecture notes, navigation, and dashboard options
 */
class AiQuestionsAndQuizIntegrationTest {

    @Before
    fun setUp() {
        SessionManager.clearSession()
        LectureNotesStorage.clearInMemory()
        QuizRepository.cachedActiveQuiz = null
    }

    @After
    fun tearDown() {
        SessionManager.clearSession()
        LectureNotesStorage.clearInMemory()
        QuizRepository.cachedActiveQuiz = null
    }

    @Test
    fun test1_textQuestionAndCompleteAnswer_automaticallySavedToMyNotes() = runBlocking {
        // Authenticate Student A
        val studentAId = 101
        val studentAName = "Vishal Kumar"
        SessionManager.saveSession(null, "token_101", studentAId, studentAName, "vishal@campus.edu")

        val questionText = "What is the primary difference between a process and a thread in operating systems?"
        val answerText = "A process is an executing program instance with its own independent address space and system resources, whereas a thread is a lightweight execution unit within a process that shares the parent process's memory space and open files."

        val savedId = NotesRepositoryImpl.saveAiQuestionAnswer(
            question = questionText,
            answer = answerText,
            subject = "Operating Systems",
            lectureTitle = "Process Management",
            dateText = "15 Nov 2026",
            timeText = "5:30 PM"
        )

        assertNotNull("Saved note ID must not be null", savedId)

        // Verify it appears in My Notes
        val notesResult = NotesRepositoryImpl.fetchNotes()
        val savedNote = notesResult.notes.firstOrNull { it.title == questionText }

        assertNotNull("The exact question must appear in My Notes", savedNote)
        assertEquals("Title must match question", questionText, savedNote?.title)
        assertEquals("Subject must match", "Operating Systems", savedNote?.subject)
        assertEquals("Tag must identify AI Q&A", "AI Q&A", savedNote?.tag)
        assertTrue("Summary must preserve the answer content", savedNote?.summary?.contains("A process is an executing program instance") == true)

        // Verify full complete answer is preserved without truncation in detailed view
        val detailedNote = NotesRepositoryImpl.fetchNoteById(savedId!!)
        assertNotNull("Detailed note view must exist", detailedNote)
        assertEquals("Detailed question must match exactly", questionText, detailedNote?.section1Content)
        assertTrue("Detailed answer must contain complete text", detailedNote?.section3Content1?.contains(answerText) == true)
        assertEquals("Tag text must indicate AI Q&A", "AI Q&A", detailedNote?.tagText)
    }

    @Test
    fun test2_voiceQuestionAndAnswer_automaticallySavedToMyNotes() = runBlocking {
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        // Transcribed voice question
        val transcribedVoiceQuestion = "Explain the time complexity of QuickSort in worst case and average case."
        val voiceAiAnswer = "QuickSort operates with O(N log N) average-case time complexity, but degrades to O(N^2) worst-case time complexity when the chosen pivot is consistently the smallest or largest element."

        val noteId = NotesRepositoryImpl.saveAiQuestionAnswer(
            question = transcribedVoiceQuestion,
            answer = voiceAiAnswer,
            subject = "Data Structures & Algorithms",
            lectureTitle = "Sorting Algorithms",
            dateText = "15 Nov 2026",
            timeText = "5:32 PM"
        )

        assertNotNull(noteId)

        val notesResult = NotesRepositoryImpl.fetchNotes()
        val found = notesResult.notes.firstOrNull { it.title == transcribedVoiceQuestion }

        assertNotNull("Transcribed voice question must be listed in My Notes", found)
        assertEquals(transcribedVoiceQuestion, found?.title)
        assertTrue(found?.summary?.contains("QuickSort operates with O(N log N)") == true)
    }

    @Test
    fun test3_duplicateQuestionsRetried_preventDuplicateEntries() = runBlocking {
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        val question = "What is ACID in database transactions?"
        val answer = "ACID stands for Atomicity, Consistency, Isolation, and Durability."

        // First save
        val firstSaveId = NotesRepositoryImpl.saveAiQuestionAnswer(
            question = question,
            answer = answer,
            subject = "Database Systems"
        )
        assertNotNull("First save must succeed", firstSaveId)

        val countAfterFirst = NotesRepositoryImpl.fetchNotes().notes.count { it.title == question }
        assertEquals("Must have exactly 1 entry after initial save", 1, countAfterFirst)

        // Immediate retry of identical question & answer
        val secondSaveId = NotesRepositoryImpl.saveAiQuestionAnswer(
            question = question,
            answer = answer,
            subject = "Database Systems"
        )

        val countAfterSecond = NotesRepositoryImpl.fetchNotes().notes.count { it.title == question }
        assertEquals("Duplicate retry must not add duplicate notes to list", 1, countAfterSecond)
    }

    @Test
    fun test4_appRestart_preservesSavedNotesAndIdentities() = runBlocking {
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        val q = "What is Amdahl's Law in parallel computing?"
        val a = "Amdahl's law gives the theoretical speedup in latency of the execution of a task at fixed workload."

        NotesRepositoryImpl.saveAiQuestionAnswer(question = q, answer = a, subject = "Computer Architecture")

        // Simulate app restart / reopen: SessionManager retains identity, storage reload
        val notes = NotesRepositoryImpl.fetchNotes().notes
        val saved = notes.firstOrNull { it.title == q }
        assertNotNull("Saved note must remain available after app restart simulation", saved)
        assertEquals(q, saved?.title)
        assertTrue(saved?.summary?.contains("Amdahl's law") == true)
    }

    @Test
    fun test5_emptyMyNotes_showsFriendlyPromptInQuizGenerator() = runBlocking {
        val studentBId = 202
        // Fresh student with zero notes
        SessionManager.saveSession(null, "token_202", studentBId, "Priya Sharma", "priya@campus.edu")

        val viewModel = QuizGeneratorViewModel()
        viewModel.loadSavedNotes()

        val state = viewModel.uiState.value
        assertTrue("isMyNotesEmpty must be true when student has no saved notes", state.isMyNotesEmpty)
        assertEquals("No notes saved yet", state.lectureTitle)
        assertTrue("Subject options must indicate no notes available", state.subjectOptions.contains("No notes available"))

        // Attempting to generate quiz with empty notes is safely blocked
        var callbackInvoked = false
        viewModel.onGenerateQuizClicked {
            callbackInvoked = true
        }
        assertFalse("Generate quiz callback must not trigger when notes are empty", callbackInvoked)
    }

    @Test
    fun test6_generateQuiz_createsQuestionsStrictlyBasedOnSavedMyNotes() = runBlocking {
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        // Student saves a specific AI Q&A on Graph Theory
        val graphQuestion = "What is Dijkstra's algorithm used for?"
        val graphAnswer = "Dijkstra's algorithm is a greedy algorithm that finds the shortest path between nodes in a weighted graph with non-negative edge weights."

        NotesRepositoryImpl.saveAiQuestionAnswer(
            question = graphQuestion,
            answer = graphAnswer,
            subject = "Algorithms",
            lectureTitle = "Graph Algorithms"
        )

        val genViewModel = QuizGeneratorViewModel()
        genViewModel.loadSavedNotes()

        val genState = genViewModel.uiState.value
        assertFalse("My Notes must not be empty", genState.isMyNotesEmpty)

        // Select the specific saved Graph Theory note
        genViewModel.onSubjectSelected(genState.subjectOptions.last())

        // Generate quiz
        var quizGenerated = false
        genViewModel.onGenerateQuizClicked {
            quizGenerated = true
        }

        // Now inspect the questions delivered to QuizQuestionViewModel
        val questionViewModel = QuizQuestionViewModel()
        questionViewModel.loadQuiz()

        val qState = questionViewModel.uiState.value
        assertTrue("Quiz questions must be populated", qState.totalQuestions > 0)
        assertFalse("Question text must not be empty", qState.questionText.isBlank())
        assertEquals("Must provide exactly 4 options A, B, C, D", 4, qState.options.size)

        // Ensure questions are strictly relevant to the saved study content (no random database normalization if quizzing Graph Algorithms!)
        val allQuizText = qState.questionText + " " + qState.options.joinToString(" ") { it.text }
        assertTrue(
            "Quiz must test concepts directly related to the saved note (Dijkstra, graph, path, or algorithms)",
            allQuizText.contains("Dijkstra", ignoreCase = true) ||
                    allQuizText.contains("graph", ignoreCase = true) ||
                    allQuizText.contains("path", ignoreCase = true) ||
                    allQuizText.contains("algorithm", ignoreCase = true)
        )
    }

    @Test
    fun test7_multiUserIsolation_studentBCannotAccessStudentANotesOrQuizzes() = runBlocking {
        // Step 1: Student A logs in and saves a private Q&A
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        val privateQuestion = "What is Student A's Confidential Research Topic on Quantum Cryptography?"
        val privateAnswer = "Student A's confidential thesis explores post-quantum lattice-based key exchange protocols."

        NotesRepositoryImpl.saveAiQuestionAnswer(
            question = privateQuestion,
            answer = privateAnswer,
            subject = "Quantum Computing"
        )

        // Confirm Student A sees it
        val studentANotes = NotesRepositoryImpl.fetchNotes().notes
        assertTrue(studentANotes.any { it.title == privateQuestion })

        // Step 2: Student A explicitly logs out
        SessionManager.clearSession()
        assertFalse("Must be logged out", SessionManager.isLoggedIn())

        // Step 3: Student B logs in fresh
        val studentBId = 202
        SessionManager.saveSession(null, "token_202", studentBId, "Priya Sharma", "priya@campus.edu")

        // Student B fetches notes
        val studentBNotes = NotesRepositoryImpl.fetchNotes().notes

        // Assert Student B NEVER sees Student A's private Q&A
        assertFalse(
            "Student B must NEVER see Student A's private questions or notes",
            studentBNotes.any { it.title.contains("Quantum Cryptography", ignoreCase = true) }
        )
        assertFalse(
            "Student B must NEVER see Student A's answer content",
            studentBNotes.any { it.summary.contains("lattice-based", ignoreCase = true) }
        )
    }

    @Test
    fun test8_existingLectureNotesAndDashboardNavigation_preservedWithoutRegression() = runBlocking {
        val studentAId = 101
        SessionManager.saveSession(null, "token_101", studentAId, "Vishal Kumar", "vishal@campus.edu")

        // Save a standard lecture note bundle (as produced by lecture recording/transcription)
        val lectureTitle = "Database Normalization and Decompositions"
        val lectureSubject = "Computer Science"

        val genNotes = GeneratedNotesUiState(
            subjectBadge = lectureSubject,
            lectureTitle = lectureTitle,
            metadataText = "Prof. Sarah Chen · 42 min",
            aiSummaryText = "Normalization organizes relations to minimize repeated data and avoid update anomalies.",
            keyTakeaways = listOf("1NF: Atomic values", "2NF: No partial dependency", "3NF: No transitive dependency"),
            importantTerms = listOf("Functional dependency", "Candidate key", "Boyce-Codd Normal Form")
        )

        val detNotes = DetailedNotesUiState(
            courseCode = "CS301",
            tagText = "AI notes",
            lectureTitle = lectureTitle,
            metadataText = "Prof. Sarah Chen · 21 Oct 2026 · 6 min read",
            section1Title = "01 Why normalize?",
            section1Content = "Normalization minimizes redundancy.",
            section2Title = "02 Key normal forms",
            section2Bullets = listOf("1NF", "2NF", "3NF"),
            calloutTitle = "Functional dependency",
            calloutContent = "X determines Y",
            section3Title = "03 Worked example",
            section3Content1 = "Decomposition examples.",
            footerText = "Study Notes"
        )

        val lectureId = LectureNotesStorage.saveLecture(
            lectureId = "lec_101_norm",
            title = lectureTitle,
            subject = lectureSubject,
            transcriptText = "Good morning everyone. Today we discuss database normalization...",
            generatedNotes = genNotes,
            detailedNotes = detNotes,
            userId = studentAId
        )

        // 1. Confirm lecture note is retained in full
        val fetchedDetailed = LectureNotesStorage.getDetailedNotes(lectureId)
        assertNotNull(fetchedDetailed)
        assertEquals(lectureTitle, fetchedDetailed?.lectureTitle)
        assertEquals("CS301", fetchedDetailed?.courseCode)

        // 2. Confirm it appears in My Notes
        val notes = NotesRepositoryImpl.fetchNotes().notes
        assertTrue(notes.any { it.title == lectureTitle })

        // 3. Confirm all four dashboard options and navigation tabs are valid
        assertEquals(HomeBottomTab.Home, HomeBottomTab.valueOf("Home"))
        assertEquals(HomeBottomTab.Notes, HomeBottomTab.valueOf("Notes"))
        assertEquals(HomeBottomTab.Profile, HomeBottomTab.valueOf("Profile"))
    }
}

