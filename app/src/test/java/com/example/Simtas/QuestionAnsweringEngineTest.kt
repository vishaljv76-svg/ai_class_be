package com.example.Simtas

import com.example.Simtas.data.ml.LectureNotesGenerator
import com.example.Simtas.data.ml.MathExpressionSolver
import com.example.Simtas.data.ml.QuestionAnsweringEngine
import com.example.Simtas.data.ml.QuestionDetector
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
 * Comprehensive Unit Tests for Automatic Question Answering and Voice-to-Text integration.
 * Tests all 7 required scenarios from the prompt:
 * 1. "What is 2 + 2?" -> Answer: 4
 * 2. "What is database normalization?" -> Relevant explanation
 * 3. "Explain artificial intelligence." -> Relevant explanation
 * 4. "Database normalization reduces data redundancy." -> Ordinary statement, no question
 * 5. Question not present in lecture -> Distinct unavailable message ("Not in lecture")
 * 6. Manually typed question -> Generates answer
 * 7. Multiple questions -> Display correct Q&A pairs without duplication
 */
class QuestionAnsweringEngineTest {

    @Before
    fun setUp() {
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
        SessionManager.setLastTranscription(null, emptyList())
    }

    @After
    fun tearDown() {
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
        SessionManager.setLastTranscription(null, emptyList())
    }

    // Test 1: "What is 2 + 2?" -> Answer: 4
    @Test
    fun test1_whatIsTwoPlusTwo_calculatesFour() {
        val question = "What is 2 + 2?"
        assertTrue("Must be recognized as a question", QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertEquals("2 + 2", result.question)
        assertEquals("4", result.answer)
        assertEquals("Adding 2 and 2 gives 4.", result.explanation)
        assertTrue(result.isAnswered)
        assertEquals("Answered", result.status)
    }

    // Test 2: "What is database normalization?" -> Relevant explanation
    @Test
    fun test2_whatIsDatabaseNormalization_returnsExplanation() {
        val question = "What is database normalization?"
        assertTrue("Must be recognized as a question", QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertTrue("Must be answered", result.isAnswered)
        assertTrue("Answer must define database normalization", result.answer.contains("Database normalization is the process of organizing data"))
        assertTrue("Must mention reducing redundancy", result.answer.contains("redundancy"))
    }

    // Test 3: "Explain artificial intelligence." -> Relevant explanation
    @Test
    fun test3_explainArtificialIntelligence_returnsExplanation() {
        val question = "Explain artificial intelligence."
        assertTrue("Directive prompt must be recognized as question", QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertTrue("Must be answered", result.isAnswered)
        assertTrue("Answer must explain AI", result.answer.contains("Artificial intelligence is the simulation of human intelligence"))
    }

    // Test 4: "Database normalization reduces data redundancy." -> Ordinary statement, no question
    @Test
    fun test4_ordinaryLectureStatement_notTreatedAsQuestion() {
        val statement = "Database normalization reduces data redundancy."
        assertFalse("Ordinary lecture statement must NOT be recognized as a question", QuestionDetector.isQuestion(statement))

        val extracted = QuestionDetector.extractQuestions(statement)
        assertTrue("No questions should be extracted from ordinary lecture statement", extracted.isEmpty())
    }

    // Test 5: Question not present in lecture -> Clearly distinguish unavailable information
    @Test
    fun test5_questionNotPresentInLecture_clearlyDistinguishesUnavailable() {
        val question = "Who was Julius Caesar?"
        val lectureContext = "Database normalization is used to organize tables and eliminate update anomalies."

        assertTrue(QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question, lectureContext)
        assertFalse("Should not claim verified answer when not in lecture", result.isAnswered)
        assertEquals("Not in lecture", result.status)
        assertTrue(result.answer.contains("This question was not covered in the current lecture content"))
    }

    // Test 6: Type a question manually -> Generate its related answer
    @Test
    fun test6_manuallyTypedQuestion_generatesRelatedAnswer() {
        val viewModel = LecturePreviewViewModel()

        // Student types a question manually
        viewModel.onRecognizedTextChanged("What is 10 * 5?")

        assertEquals("What is 10 * 5?", viewModel.uiState.value.recognizedText)

        // Directly evaluate question
        val result = QuestionAnsweringEngine.answerQuestion("What is 10 * 5?")
        assertEquals("10 * 5", result.question)
        assertEquals("50", result.answer)
        assertEquals("Multiplying 10 by 5 gives 50.", result.explanation)
    }

    // Test 7: Speak multiple questions -> Display correct Q&A pairs without duplication
    @Test
    fun test7_multipleQuestions_distinctQuestionAnswerPairs() {
        val speech = "What is 2 + 2?\nWhat is 10 * 5?\nWhat is 2 + 2?"

        val questions = QuestionDetector.extractQuestions(speech)
        // Deduplicated: should only have 2 unique questions
        assertEquals(2, questions.size)

        val answers = questions.map { QuestionAnsweringEngine.answerQuestion(it) }
        assertEquals(2, answers.size)

        assertEquals("2 + 2", answers[0].question)
        assertEquals("4", answers[0].answer)

        assertEquals("10 * 5", answers[1].question)
        assertEquals("50", answers[1].answer)
    }

    // Extra: Standalone math expression without question words (e.g. "2+2")
    @Test
    fun testRawMathExpressionWithoutQuestionWords() {
        assertTrue(QuestionDetector.isQuestion("2+2"))
        val result = QuestionAnsweringEngine.answerQuestion("2+2")
        assertEquals("2 + 2", result.question)
        assertEquals("4", result.answer)
    }

    // Extra: Integration with LectureNotesGenerator
    @Test
    fun testGeneratedNotesIncludesQuestionsAndAnswers() {
        val transcript = "What is database normalization?\nDatabase normalization is used to organize data."
        val result = LectureNotesGenerator.generate(transcript)

        val genNotes = result.generatedNotes
        assertTrue("Generated notes must include questions and answers", genNotes.detectedQuestionsAndAnswers.isNotEmpty())
        val firstQa = genNotes.detectedQuestionsAndAnswers[0]
        assertTrue(firstQa.question.contains("normalization"))
        assertTrue(firstQa.isAnswered)
    }

    // Bug Fix Test: "Name, some domestic and wild animals." must generate animal notes, not Database Normalization
    @Test
    fun testAnimalsTranscript_accurateTitleSubjectAndNoRepetition() {
        val transcript = "Name, some domestic and wild animals."

        // Even if default title passed in is "Database Normalization"
        val result = LectureNotesGenerator.generate(
            transcriptText = transcript,
            lectureTitle = "Database Normalization"
        )
        val genNotes = result.generatedNotes

        // 1. Title must NOT be Database Normalization
        assertEquals("Domestic and Wild Animals", genNotes.lectureTitle)

        // 2. Subject must be Biology
        assertEquals("Biology", genNotes.subjectBadge)

        // 3. AI Summary must synthesize the topic and answer
        assertTrue("Summary must discuss domestic and wild animals", genNotes.aiSummaryText.contains("domestic", ignoreCase = true))
        assertTrue("Summary must discuss wild animals", genNotes.aiSummaryText.contains("wild", ignoreCase = true))

        // 4. Key Takeaways must NOT be identical to the transcript or summary
        assertTrue("Must have key takeaways", genNotes.keyTakeaways.isNotEmpty())
        for (takeaway in genNotes.keyTakeaways) {
            assertFalse("Takeaway must not duplicate summary verbatim", takeaway == genNotes.aiSummaryText)
            assertFalse("Takeaway must not be raw unformatted transcript", takeaway == transcript)
        }

        // 5. Questions and Answers section must contain the answered question
        assertTrue("Must have detected Q&A", genNotes.detectedQuestionsAndAnswers.isNotEmpty())
        val qa = genNotes.detectedQuestionsAndAnswers[0]
        assertTrue(qa.isAnswered)
        assertTrue(qa.answer.contains("Domestic animals", ignoreCase = true))
        assertTrue(qa.answer.contains("Wild animals", ignoreCase = true))

        // 6. Important terms must be relevant to animals
        assertTrue(genNotes.importantTerms.contains("Domestic Animals"))
        assertTrue(genNotes.importantTerms.contains("Wild Animals"))
    }

    // Bug Fix Test: Empty transcript must display honest clear message with zero invented facts
    @Test
    fun testEmptyTranscript_honestMessageAndZeroFakeFacts() {
        val result = LectureNotesGenerator.generate(
            transcriptText = "",
            lectureTitle = "Database Normalization"
        )
        val genNotes = result.generatedNotes

        // Title should be neutral recorded lecture, not Database Normalization
        assertEquals("Recorded Lecture Notes", genNotes.lectureTitle)
        assertEquals("General Studies", genNotes.subjectBadge)
        assertTrue(genNotes.aiSummaryText.contains("No spoken content detected"))
        assertTrue(genNotes.keyTakeaways.isNotEmpty())
        assertTrue(genNotes.keyTakeaways[0].contains("No speech or notes could be extracted"))
        assertTrue(genNotes.importantTerms.isEmpty())
        assertTrue(genNotes.detectedQuestionsAndAnswers.isEmpty())
    }

    // Bug Fix Test: Multi-sentence lecture summary must not duplicate takeaways
    @Test
    fun testMultiSentenceLecture_noRepetitionBetweenSummaryAndTakeaways() {
        val transcript = "Database normalization is the process of structuring relational tables. " +
            "It minimizes data redundancy and functional dependency anomalies. " +
            "The primary objective is to maintain database integrity across modifications. " +
            "Normal forms provide progressive guidelines for relational schema design."

        val result = LectureNotesGenerator.generate(
            transcriptText = transcript,
            lectureTitle = "Database Normalization"
        )
        val genNotes = result.generatedNotes

        assertEquals("Database Normalization", genNotes.lectureTitle)
        assertEquals("Computer Science", genNotes.subjectBadge)

        // Verify summary exists
        assertTrue(genNotes.aiSummaryText.isNotBlank())

        // Verify takeaways are non-empty and mutually distinct from summary
        assertTrue(genNotes.keyTakeaways.isNotEmpty())
        for (takeaway in genNotes.keyTakeaways) {
            assertFalse("Takeaway must not equal summary verbatim", takeaway == genNotes.aiSummaryText)
        }
    }
}
