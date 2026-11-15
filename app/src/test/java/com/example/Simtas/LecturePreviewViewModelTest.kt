package com.example.Simtas

import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.preview.LecturePreviewViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Screen 2: [LecturePreviewViewModel] verifying voice-to-text,
 * manual text input, combined input, language selection, and session persistence.
 */
class LecturePreviewViewModelTest {

    @Before
    fun setUp() {
        SessionManager.clearSession()
        SessionManager.setLastTranscription(null, emptyList())
        SessionManager.setLastRecordedAudioPath(null)
    }

    @After
    fun tearDown() {
        SessionManager.clearSession()
        SessionManager.setLastTranscription(null, emptyList())
        SessionManager.setLastRecordedAudioPath(null)
    }

    @Test
    fun testInitialState_defaultValues() {
        val viewModel = LecturePreviewViewModel()
        val state = viewModel.uiState.value

        assertEquals("Database Normalization", state.lectureTitle)
        assertEquals("en", state.selectedLanguageCode)
        assertFalse(state.isListening)
        assertFalse(state.isTranscribing)
        assertEquals("", state.recognizedText)
    }

    @Test
    fun testManualTextInput_updatesStateAndSessionManager() {
        val viewModel = LecturePreviewViewModel()
        val typedText = "Database normalization is used to organize data."

        viewModel.onRecognizedTextChanged(typedText)

        assertEquals(typedText, viewModel.uiState.value.recognizedText)
        assertEquals(typedText, SessionManager.getLastTranscriptionText())
        val paragraphs = SessionManager.getLastTranscriptionParagraphs()
        assertTrue("Paragraphs should not be empty", paragraphs.isNotEmpty())
        assertEquals(typedText, paragraphs[0].text)
    }

    @Test
    fun testCombinedVoiceAndManualText_preservesBothInputs() {
        val viewModel = LecturePreviewViewModel()

        // 1. First input (simulating recognized speech)
        val spokenSpeech = "Database normalization is used to organize data."
        viewModel.onRecognizedTextChanged(spokenSpeech)
        assertEquals(spokenSpeech, viewModel.uiState.value.recognizedText)

        // 2. Student manually adds a question
        val combined = "$spokenSpeech\nExplain this topic."
        viewModel.onRecognizedTextChanged(combined)

        assertEquals(combined, viewModel.uiState.value.recognizedText)
        assertEquals(combined, SessionManager.getLastTranscriptionText())
        val paragraphs = SessionManager.getLastTranscriptionParagraphs()
        assertEquals(2, paragraphs.size)
        assertEquals("Database normalization is used to organize data.", paragraphs[0].text)
        assertEquals("Explain this topic.", paragraphs[1].text)
    }

    @Test
    fun testClearText_resetsTextAndSession() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onRecognizedTextChanged("Some content")
        assertEquals("Some content", viewModel.uiState.value.recognizedText)

        viewModel.onClearTextClicked()
        assertEquals("", viewModel.uiState.value.recognizedText)
        assertEquals("", SessionManager.getLastTranscriptionText())
        assertTrue(SessionManager.getLastTranscriptionParagraphs().isEmpty())
    }

    @Test
    fun testLanguageSelection_switchesBetweenEnglishAndTamil() {
        val viewModel = LecturePreviewViewModel()
        assertEquals("en", viewModel.uiState.value.selectedLanguageCode)

        viewModel.onLanguageSelected("ta")
        assertEquals("ta", viewModel.uiState.value.selectedLanguageCode)

        viewModel.onLanguageSelected("en")
        assertEquals("en", viewModel.uiState.value.selectedLanguageCode)
    }

    @Test
    fun testGenerateTranscriptClicked_validatesInput() {
        val viewModel = LecturePreviewViewModel()

        // Blank text and no file -> returns false with error
        val canProceedEmpty = viewModel.onGenerateTranscriptClicked()
        assertFalse(canProceedEmpty)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Valid text -> returns true and stores to session
        viewModel.onRecognizedTextChanged("Lecture on algorithms")
        val canProceedValid = viewModel.onGenerateTranscriptClicked()
        assertTrue(canProceedValid)
        assertEquals("Lecture on algorithms", SessionManager.getLastTranscriptionText())
    }

    @Test
    fun testReopeningScreen_restoresPreviouslySavedText() {
        // Given text was saved previously in session
        SessionManager.setLastTranscription(
            "Previously saved lecture note",
            emptyList()
        )

        // When a new LecturePreviewViewModel is created
        val newViewModel = LecturePreviewViewModel()

        // Then previous text is restored
        assertEquals("Previously saved lecture note", newViewModel.uiState.value.recognizedText)
    }

    @Test
    fun testExtractLectureIntelligence_generatesExplanationSummaryAndStudyQuestions() {
        val viewModel = LecturePreviewViewModel()
        val lectureTranscript = "Database normalization is the process of organizing data in a database. " +
            "First normal form eliminates repeating groups. Second normal form removes partial dependencies."

        viewModel.onRecognizedTextChanged(lectureTranscript)

        // Give coroutine a moment to complete
        Thread.sleep(150)

        val state = viewModel.uiState.value
        assertTrue("Teacher explanation should be generated", state.teacherExplanation.isNotBlank())
        assertTrue("Lecture summary should be generated", state.lectureSummary.isNotBlank())
        assertTrue("Key takeaways should be populated", state.keyTakeaways.isNotEmpty())
        assertTrue("Study questions should be generated", state.studyQuestionsAndAnswers.isNotEmpty())

        // Verify that study questions are tagged with From Lecture
        val lectureItem = state.studyQuestionsAndAnswers.first()
        assertTrue("Study Q&A source should indicate from lecture", lectureItem.source.contains("Lecture"))
    }

    @Test
    fun testFollowUpQuestions_differentiatesBetweenLectureAndSupplementalAnswers() {
        val viewModel = LecturePreviewViewModel()
        val lectureTranscript = "Photosynthesis is the process by which plants use sunlight to convert water and carbon dioxide into oxygen and glucose."

        viewModel.onRecognizedTextChanged(lectureTranscript)
        Thread.sleep(150)

        // 1. Ask a question directly covered in the lecture
        viewModel.onFollowUpQuestionChanged("What is photosynthesis?")
        viewModel.askFollowUpQuestion()

        val qaList = viewModel.uiState.value.followUpQaList
        assertEquals(1, qaList.size)
        val firstQa = qaList[0]
        assertEquals("What is photosynthesis?", firstQa.question)
        assertTrue("Answer should be answered", firstQa.isAnswered)
        assertTrue("Source should be [From Lecture]", firstQa.source.contains("Lecture"))

        // 2. Ask a question not in lecture (should fall back to supplemental student knowledge or indicate not in lecture)
        viewModel.onFollowUpQuestionChanged("What is the time complexity of quicksort?")
        viewModel.askFollowUpQuestion()

        val updatedList = viewModel.uiState.value.followUpQaList
        assertEquals(2, updatedList.size)
        val secondQa = updatedList[1]
        assertTrue("Source should indicate Supplemental or General Knowledge", 
            secondQa.source.contains("Supplemental") || secondQa.source.contains("Knowledge") || secondQa.source.contains("Not in Lecture"))
    }

    @Test
    fun testTamilLanguageSupport_processesTamilContentAndQuestions() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onLanguageSelected("ta")

        val tamilTranscript = "தரவுத்தள இயல்பாக்கம் என்பது தேவையற்ற தரவை நீக்கும் முறையாகும்."
        viewModel.onRecognizedTextChanged(tamilTranscript)
        Thread.sleep(150)

        val state = viewModel.uiState.value
        assertEquals("ta", state.selectedLanguageCode)
        assertTrue("Teacher explanation should be generated for Tamil lecture", state.teacherExplanation.isNotBlank())
    }

    @Test
    fun testTranscriptionRetry_clearsErrorStateAndTriggersRetry() {
        val viewModel = LecturePreviewViewModel()
        // Simulate failure state
        viewModel.onRecognizedTextChanged("")
        assertFalse(viewModel.uiState.value.transcriptionFailed)
    }

    @Test
    fun testStudyQuestions_generatesFiveToTenQuestions() {
        val viewModel = LecturePreviewViewModel()
        val lectureTranscript = "Database normalization organizes data into tables to reduce redundancy. " +
            "First normal form removes duplicate columns. Second normal form removes partial functional dependencies. " +
            "Third normal form ensures non-key attributes depend only on the primary key."

        viewModel.onRecognizedTextChanged(lectureTranscript)
        Thread.sleep(150)

        val state = viewModel.uiState.value
        assertTrue(
            "Study questions count should be between 5 and 10, got ${state.studyQuestionsAndAnswers.size}",
            state.studyQuestionsAndAnswers.size in 5..10
        )
    }

    @Test
    fun testRetryLectureAnalysis_triggersReanalysis() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onRecognizedTextChanged("Operating systems manage hardware and software resources.")
        Thread.sleep(150)

        viewModel.retryLectureAnalysis()
        Thread.sleep(150)

        val state = viewModel.uiState.value
        assertTrue("Teacher explanation should be present after retry", state.teacherExplanation.isNotBlank())
        assertTrue("Study questions should be present after retry", state.studyQuestionsAndAnswers.isNotEmpty())
    }

    @Test
    fun testDataStructuresLecture_generatesMeaningfulQuestionsAndDetailedAnswers() {
        val viewModel = LecturePreviewViewModel()
        val dataStructuresTranscript = "Good morning class. Today we will explore Data Structures. " +
            "A data structure is a specialized format for organizing, storing, and retrieving data efficiently in computer memory. " +
            "Data structures are broadly divided into linear data structures, like arrays, linked lists, stacks, and queues, " +
            "and non-linear data structures, such as trees and graphs. In an array, elements are stored in contiguous memory locations " +
            "with constant time index access, whereas in a linked list, elements are connected via pointers. Stacks follow the " +
            "Last-In First-Out principle, while queues follow the First-In First-Out principle. Understanding these differences and " +
            "trade-offs is essential for writing efficient algorithms."

        viewModel.onRecognizedTextChanged(dataStructuresTranscript)
        Thread.sleep(200)

        val state = viewModel.uiState.value
        val questions = state.studyQuestionsAndAnswers

        // 1. Verify 5-10 meaningful questions
        assertTrue("Generated questions count should be between 5 and 10, got ${questions.size}", questions.size in 5..10)

        // 2. Verify EVERY question has a complete, detailed answer
        for (q in questions) {
            assertTrue("Question text should not be blank", q.question.isNotBlank())
            assertTrue("Every question MUST have a complete answer (got: '${q.answer}')", q.answer.length >= 15)
            assertTrue("Every question should be answered", q.isAnswered)
            assertTrue("Source should be clearly marked", q.source.isNotBlank())

            // 3. Verify NO meaningless filler questions from words like "Students", "Today", "Class", "Morning"
            val lowerQ = q.question.lowercase(java.util.Locale.ROOT)
            assertFalse("Should never generate meaningless filler question: ${q.question}",
                lowerQ.contains("what is students") ||
                lowerQ.contains("what is class") ||
                lowerQ.contains("what is today") ||
                lowerQ.contains("what is morning")
            )
        }

        // 4. Verify question coverage of diverse styles (concept, types, characteristics, differences, examples)
        val allQuestionsCombined = questions.joinToString(" ") { it.question.lowercase(java.util.Locale.ROOT) }
        assertTrue("Should include concept definition or types or differences question",
            allQuestionsCombined.contains("what is") ||
            allQuestionsCombined.contains("types") ||
            allQuestionsCombined.contains("characteristics") ||
            allQuestionsCombined.contains("differentiate") ||
            allQuestionsCombined.contains("examples")
        )
    }

    @Test
    fun testTamilDataStructuresLecture_generatesMeaningfulTamilQuestionsAndDetailedAnswers() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onLanguageSelected("ta")

        val tamilTranscript = "வணக்கம் மாணவர்களே. இன்று நாம் தரவு அமைப்புகள் (Data Structures) பற்றி பார்க்க போகிறோம். " +
            "தரவு அமைப்பு என்பது கணினி நினைவகத்தில் தரவை திறமையாக சேமிக்கவும் ஒழுங்கமைக்கவும் பயன்படும் முறையாகும். " +
            "நேரியல் தரவு அமைப்புகள் மற்றும் நேரியல் அல்லாத தரவு அமைப்புகள் என இரு பெரும் பிரிவுகள் உள்ளன."

        viewModel.onRecognizedTextChanged(tamilTranscript)
        Thread.sleep(200)

        val state = viewModel.uiState.value
        val questions = state.studyQuestionsAndAnswers

        assertTrue("Generated questions in Tamil should be between 5 and 10, got ${questions.size}", questions.size in 5..10)
        for (q in questions) {
            assertTrue("Question text should not be blank", q.question.isNotBlank())
            assertTrue("Every question in Tamil MUST have a detailed answer", q.answer.length >= 10)
            assertTrue("Question should be marked answered", q.isAnswered)

            val lowerQ = q.question.lowercase(java.util.Locale.ROOT)
            assertFalse("Should not generate meaningless Tamil filler questions",
                lowerQ.contains("மாணவர்கள் என்றால் என்ன") || lowerQ.contains("வணக்கம் என்றால் என்ன")
            )
        }
    }

    @Test
    fun testScreenshotIntroTranscript_identifiesDataStructuresAndGeneratesDetailedQuestions() {
        val viewModel = LecturePreviewViewModel()
        val screenshotTranscript = "Good morning to everyone. Today we are going to see about data structures."

        viewModel.onRecognizedTextChanged(screenshotTranscript)
        Thread.sleep(200)

        val state = viewModel.uiState.value

        // 1. Subject/Topic Identification: Must resolve to Data Structures, NOT default Database Normalization
        assertEquals("Lecture title must be dynamically resolved to Data Structures", "Data Structures", state.lectureTitle)

        // 2. Loading State: isAnsweringQuestion MUST be false after analysis completes
        assertFalse("Progress spinner 'Analyzing question...' must be dismissed", state.isAnsweringQuestion)

        // 3. Question Count: 5 to 10 meaningful questions
        val questions = state.studyQuestionsAndAnswers
        assertTrue("Generated questions count should be between 5 and 10, got ${questions.size}", questions.size in 5..10)

        // 4. Grounding & Distinction: Question 1 is [From Lecture], Questions 2-7 are [Supplemental AI Explanation]
        val firstQ = questions[0]
        assertEquals("[From Lecture]", firstQ.source)
        assertTrue("First question should ask about the introduced lecture topic",
            firstQ.question.contains("topic", ignoreCase = true) || firstQ.question.contains("introduced", ignoreCase = true)
        )
        assertTrue("First answer should identify Data Structures", firstQ.answer.contains("Data Structures", ignoreCase = true))

        for (i in 1 until questions.size) {
            val q = questions[i]
            assertEquals("Supplemental question should be marked [Supplemental AI Explanation]", "[Supplemental AI Explanation]", q.source)
            assertTrue("Every question MUST have a complete, detailed answer (got: '${q.answer}')", q.answer.length >= 30)
            assertFalse("Answer must NOT simply repeat the raw transcript verbatim", q.answer.trim().equals(screenshotTranscript, ignoreCase = true))
        }

        // 5. Strict Negative Rules: No Database Normalization or conversational filler questions
        for (q in questions) {
            val lowerQ = q.question.lowercase(java.util.Locale.ROOT)
            val lowerA = q.answer.lowercase(java.util.Locale.ROOT)
            assertFalse("Must not contain Database Normalization hallucination in question: ${q.question}", lowerQ.contains("database normalization"))
            assertFalse("Must not contain Database Normalization hallucination in answer: ${q.answer}", lowerA.contains("database normalization"))
            assertFalse("Must not generate 'What is Today we are': ${q.question}", lowerQ.contains("what is today"))
            assertFalse("Must not generate 'What is Good morning': ${q.question}", lowerQ.contains("what is good morning"))
        }

        // 6. Teacher Explanation & Summary
        assertTrue("Teacher explanation must be generated and non-blank", state.teacherExplanation.isNotBlank())
        assertTrue("Teacher explanation must mention Data Structures", state.teacherExplanation.contains("Data Structures"))
        assertTrue("Lecture summary must be non-blank", state.lectureSummary.isNotBlank())
    }

    @Test
    fun testTamilScreenshotIntroTranscript_identifiesDataStructuresAndGeneratesTamilQuestions() {
        val viewModel = LecturePreviewViewModel()
        viewModel.onLanguageSelected("ta")

        val tamilScreenshotTranscript = "வணக்கம் அனைவருக்கும். இன்று நாம் தரவு அமைப்புகள் பற்றி பார்க்க போகிறோம்."
        viewModel.onRecognizedTextChanged(tamilScreenshotTranscript)
        Thread.sleep(200)

        val state = viewModel.uiState.value

        // 1. Topic Identification in Tamil
        assertTrue("Lecture title should be identified as Data Structures or தரவு அமைப்புகள், got: ${state.lectureTitle}",
            state.lectureTitle.contains("தரவு அமைப்பு") || state.lectureTitle.equals("Data Structures", ignoreCase = true)
        )

        // 2. Loading State must be dismissed
        assertFalse("Progress spinner 'Analyzing question...' must be dismissed", state.isAnsweringQuestion)

        // 3. Question Count: 5 to 10 questions
        val questions = state.studyQuestionsAndAnswers
        assertTrue("Tamil questions count should be between 5 and 10, got ${questions.size}", questions.size in 5..10)

        // 4. Grounding
        assertEquals("[From Lecture]", questions[0].source)
        for (i in 1 until questions.size) {
            assertEquals("[Supplemental AI Explanation]", questions[i].source)
            assertTrue("Tamil answer must be detailed", questions[i].answer.length >= 20)
        }

        // 5. Negative Rules
        for (q in questions) {
            val lowerQ = q.question.lowercase(java.util.Locale.ROOT)
            assertFalse("Must not hallucinate Database Normalization in Tamil: ${q.question}", lowerQ.contains("database normalization") || lowerQ.contains("தரவுத்தள இயல்பாக்கம்"))
            assertFalse("Must not generate filler question: ${q.question}", lowerQ.contains("வணக்கம் என்றால் என்ன") || lowerQ.contains("இன்று நாம் என்றால் என்ன"))
        }
    }
}

