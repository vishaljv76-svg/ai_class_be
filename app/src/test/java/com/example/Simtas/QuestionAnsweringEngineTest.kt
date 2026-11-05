package com.example.Simtas

import com.example.Simtas.data.local.LectureNotesStorage
import com.example.Simtas.data.ml.LectureNotesGenerator
import com.example.Simtas.data.ml.MathExpressionEvaluator
import com.example.Simtas.data.ml.MathExpressionSolver
import com.example.Simtas.data.ml.OfflineReasoningEvaluator
import com.example.Simtas.data.ml.QuestionAnsweringEngine
import com.example.Simtas.data.ml.QuestionDetector
import com.example.Simtas.data.ml.TranscriptQaRetriever
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.preview.LecturePreviewViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Tests for Real-Time AI Notes and Question Answering.
 * Verifies all requirements:
 * 1. Test A: Database Normalization (Notes, What is normalization?, What is 1NF?, What is 2NF?)
 * 2. Test B: Animal Biology (Notes, What are wild animals?, Distinct from Test A)
 * 3. Math Questions: "What is 2 + 2? -> 4", "Solve 15 × 5. -> 75"
 * 4. Comparative Reasoning: "What is the difference between 1NF and 2NF?"
 * 5. Distinct answers for different questions
 * 6. Empty transcripts do not produce fake notes
 * 7. Separate study notes persistence: Reopening old lectures loads their own saved notes
 */
class QuestionAnsweringEngineTest {

    @Before
    fun setUp() {
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
        SessionManager.setLastTranscription(null, emptyList())
        LectureNotesStorage.clearInMemory()
    }

    @After
    fun tearDown() {
        SessionManager.setDetectedQuestionsAndAnswers(emptyList())
        SessionManager.setLastTranscription(null, emptyList())
        LectureNotesStorage.clearInMemory()
    }

    // --- Basic QA & Math Tests ---

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

    @Test
    fun testSolve15Times5_returns75() {
        val question = "Solve 15 × 5."
        assertTrue(QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertEquals("15 × 5", result.question)
        assertEquals("75", result.answer)
        assertEquals("Multiplying 15 by 5 gives 75.", result.explanation)
        assertTrue(result.isAnswered)
    }

    @Test
    fun test2_whatIsDatabaseNormalization_returnsExplanation() {
        val question = "What is database normalization?"
        assertTrue("Must be recognized as a question", QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertTrue("Must be answered", result.isAnswered)
        assertTrue("Answer must define database normalization", result.answer.contains("Database normalization is the process of organizing data", ignoreCase = true))
        assertTrue("Must mention reducing redundancy", result.answer.contains("redundancy", ignoreCase = true))
    }

    @Test
    fun test3_explainArtificialIntelligence_returnsExplanation() {
        val question = "Explain artificial intelligence."
        assertTrue("Directive prompt must be recognized as question", QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertTrue("Must be answered", result.isAnswered)
        assertTrue("Answer must explain AI", result.answer.contains("Artificial intelligence is the simulation of human intelligence"))
    }

    @Test
    fun test4_ordinaryLectureStatement_notTreatedAsQuestion() {
        val statement = "Database normalization reduces data redundancy."
        assertFalse("Ordinary lecture statement must NOT be recognized as a question", QuestionDetector.isQuestion(statement))

        val extracted = QuestionDetector.extractQuestions(statement)
        assertTrue("No questions should be extracted from ordinary lecture statement", extracted.isEmpty())
    }

    @Test
    fun test5_questionNotPresentInLecture_clearlyDistinguishesUnavailable() {
        val question = "Who was Julius Caesar?"
        val lectureTranscript = "Database normalization is used to organize tables and eliminate update anomalies."

        assertTrue(QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(
            question = question,
            transcriptText = lectureTranscript
        )
        assertFalse("Should not claim verified answer when not in lecture", result.isAnswered)
        assertEquals("Not in lecture", result.status)
        assertTrue(result.answer.contains("This question was not covered in the current lecture content"))
    }

    @Test
    fun testDifferenceBetween1NFAnd2NF() {
        val question = "What is the difference between 1NF and 2NF?"
        assertTrue(QuestionDetector.isQuestion(question))

        val result = QuestionAnsweringEngine.answerQuestion(question)
        assertTrue(result.isAnswered)
        assertTrue("Must explain 1NF requirements", result.answer.contains("1NF", ignoreCase = true))
        assertTrue("Must explain 2NF requirements", result.answer.contains("2NF", ignoreCase = true))
        assertTrue("Must mention partial functional dependencies", result.answer.contains("partial functional dependencies", ignoreCase = true))
    }

    @Test
    fun testDifferentQuestionsReceiveDifferentAnswers() {
        val q1 = "What is 1NF?"
        val q2 = "What is 2NF?"

        val ans1 = QuestionAnsweringEngine.answerQuestion(q1)
        val ans2 = QuestionAnsweringEngine.answerQuestion(q2)

        assertTrue(ans1.isAnswered)
        assertTrue(ans2.isAnswered)
        assertNotEquals("Different questions must receive different answers", ans1.answer, ans2.answer)
        assertTrue(ans1.answer.contains("atomic", ignoreCase = true))
        assertTrue(ans2.answer.contains("partial functional dependenc", ignoreCase = true))
    }

    // --- Test A: Database Normalization Workflow & Questions ---

    @Test
    fun testA_DatabaseNormalization_WorkflowAndQuestions() {
        val transcript = "Database normalization is defined as the process of organizing data in a database. " +
            "First normal form (1NF) requires atomic values in each table cell with no repeating groups. " +
            "Second normal form (2NF) removes partial functional dependencies so that non-key attributes depend on the entire primary key. " +
            "Third normal form (3NF) eliminates transitive dependencies between non-key attributes. " +
            "This reduces data redundancy and improves database integrity."

        // 1. Generate Notes
        val notesResult = LectureNotesGenerator.generate(
            transcriptText = transcript,
            lectureTitle = "Database Normalization"
        )
        val genNotes = notesResult.generatedNotes
        val detailedNotes = notesResult.detailedNotes

        assertEquals("Database Normalization", genNotes.lectureTitle)
        assertEquals("Computer Science", genNotes.subjectBadge)

        // Verify summary contains core purpose
        assertTrue(genNotes.aiSummaryText.contains("normalization", ignoreCase = true))

        // Verify key takeaways are extracted and distinct from summary
        assertTrue(genNotes.keyTakeaways.isNotEmpty())
        for (takeaway in genNotes.keyTakeaways) {
            assertNotEquals(genNotes.aiSummaryText, takeaway)
        }

        // Verify detailed notes structure
        assertTrue(detailedNotes.section1Content.isNotBlank())
        assertTrue("Section 2 bullets must be present", detailedNotes.section2Bullets.isNotEmpty())

        // 2. Ask "What is normalization?" -> Answered from transcript
        val ansNormalization = QuestionAnsweringEngine.answerQuestion(
            question = "What is normalization?",
            transcriptText = transcript,
            detailedNotes = detailedNotes,
            summaryText = genNotes.aiSummaryText,
            keyTakeaways = genNotes.keyTakeaways
        )
        assertTrue("Must be answered", ansNormalization.isAnswered)
        assertTrue("Must explain normalization", ansNormalization.answer.contains("process of organizing data", ignoreCase = true))
        assertEquals("Answered from Transcript", ansNormalization.status)

        // 3. Ask "What is 1NF?" -> Answered from transcript/study notes
        val ans1NF = QuestionAnsweringEngine.answerQuestion(
            question = "What is 1NF?",
            transcriptText = transcript,
            detailedNotes = detailedNotes,
            summaryText = genNotes.aiSummaryText,
            keyTakeaways = genNotes.keyTakeaways
        )
        assertTrue("Must be answered", ans1NF.isAnswered)
        assertTrue("Answer must mention atomic values", ans1NF.answer.contains("atomic", ignoreCase = true))

        // 4. Ask "What is 2NF?" -> Answered from transcript/study notes
        val ans2NF = QuestionAnsweringEngine.answerQuestion(
            question = "What is 2NF?",
            transcriptText = transcript,
            detailedNotes = detailedNotes,
            summaryText = genNotes.aiSummaryText,
            keyTakeaways = genNotes.keyTakeaways
        )
        assertTrue("Must be answered", ans2NF.isAnswered)
        assertTrue("Answer must mention partial functional dependencies", ans2NF.answer.contains("partial", ignoreCase = true))

        // Verify answers to 1NF and 2NF are distinct
        assertNotEquals(ans1NF.answer, ans2NF.answer)
    }

    // --- Test B: Animal Biology Workflow & Questions ---

    @Test
    fun testB_AnimalBiology_WorkflowAndQuestions() {
        val transcript = "Domestic animals are animals kept and cared for by humans for companionship and agriculture. " +
            "Wild animals live independently in natural habitats such as forests and oceans without human management. " +
            "Both domestic and wild species adapt their behaviors to survive in different ecosystems."

        // 1. Generate Notes
        val notesResult = LectureNotesGenerator.generate(
            transcriptText = transcript,
            lectureTitle = "Recorded Lecture" // Should auto-detect title
        )
        val genNotes = notesResult.generatedNotes
        val detailedNotes = notesResult.detailedNotes

        // Title and Subject must reflect Animal Biology
        assertEquals("Biology", genNotes.subjectBadge)
        assertTrue(genNotes.lectureTitle.contains("Animals", ignoreCase = true) || genNotes.lectureTitle.contains("Domestic", ignoreCase = true))

        // Verify no Database Normalization leakage into Animal Biology
        assertFalse("Must NOT contain database concepts", genNotes.aiSummaryText.contains("normalization", ignoreCase = true))
        assertFalse("Must NOT contain database concepts", genNotes.aiSummaryText.contains("relational", ignoreCase = true))

        // 2. Ask "What are wild animals?" -> Answered from transcript
        val ansWildAnimals = QuestionAnsweringEngine.answerQuestion(
            question = "What are wild animals?",
            transcriptText = transcript,
            detailedNotes = detailedNotes,
            summaryText = genNotes.aiSummaryText,
            keyTakeaways = genNotes.keyTakeaways
        )
        assertTrue(ansWildAnimals.isAnswered)
        assertTrue(ansWildAnimals.answer.contains("Wild animals live independently", ignoreCase = true))
        assertEquals("Answered from Transcript", ansWildAnimals.status)

        // 3. Ask "What are domestic animals?" -> Answered from transcript
        val ansDomesticAnimals = QuestionAnsweringEngine.answerQuestion(
            question = "What are domestic animals?",
            transcriptText = transcript,
            detailedNotes = detailedNotes,
            summaryText = genNotes.aiSummaryText,
            keyTakeaways = genNotes.keyTakeaways
        )
        assertTrue(ansDomesticAnimals.isAnswered)
        assertTrue(ansDomesticAnimals.answer.contains("Domestic animals are animals kept", ignoreCase = true))

        assertNotEquals(ansWildAnimals.answer, ansDomesticAnimals.answer)
    }

    // --- Separate Study Notes: Saving & Reopening ---

    @Test
    fun testSeparateStudyNotes_SavingAndReloading() {
        // Lecture 1: Database Normalization
        val transcript1 = "Database normalization reduces data redundancy and improves database integrity."
        val notes1 = LectureNotesGenerator.generate(transcript1, lectureTitle = "Database Normalization")
        val id1 = "lecture_db_101"

        LectureNotesStorage.saveLecture(
            lectureId = id1,
            title = notes1.generatedNotes.lectureTitle,
            subject = notes1.generatedNotes.subjectBadge,
            transcriptText = transcript1,
            generatedNotes = notes1.generatedNotes,
            detailedNotes = notes1.detailedNotes
        )

        // Lecture 2: Animal Biology
        val transcript2 = "Domestic animals are cared for by humans, while wild animals survive independently in natural ecosystems."
        val notes2 = LectureNotesGenerator.generate(transcript2, lectureTitle = "Animal Biology")
        val id2 = "lecture_bio_202"

        LectureNotesStorage.saveLecture(
            lectureId = id2,
            title = notes2.generatedNotes.lectureTitle,
            subject = notes2.generatedNotes.subjectBadge,
            transcriptText = transcript2,
            generatedNotes = notes2.generatedNotes,
            detailedNotes = notes2.detailedNotes
        )

        // Reopen Lecture 1 -> Must load Lecture 1's own saved notes
        val reloadedDetailed1 = LectureNotesStorage.getDetailedNotes(id1)
        val reloadedGen1 = LectureNotesStorage.getGeneratedNotes(id1)
        assertNotNull(reloadedDetailed1)
        assertNotNull(reloadedGen1)
        assertEquals("Database Normalization", reloadedDetailed1?.lectureTitle)
        assertEquals("Computer Science", reloadedGen1?.subjectBadge)
        assertTrue(reloadedGen1?.aiSummaryText?.contains("redundancy", ignoreCase = true) == true)

        // Reopen Lecture 2 -> Must load Lecture 2's own saved notes
        val reloadedDetailed2 = LectureNotesStorage.getDetailedNotes(id2)
        val reloadedGen2 = LectureNotesStorage.getGeneratedNotes(id2)
        assertNotNull(reloadedDetailed2)
        assertNotNull(reloadedGen2)
        assertEquals("Animal Biology", reloadedDetailed2?.lectureTitle)
        assertEquals("Biology", reloadedGen2?.subjectBadge)
        assertTrue(reloadedGen2?.aiSummaryText?.contains("animals", ignoreCase = true) == true)

        // Verify neither lecture overwrote the other
        assertNotEquals(reloadedDetailed1?.lectureTitle, reloadedDetailed2?.lectureTitle)
        assertNotEquals(reloadedGen1?.subjectBadge, reloadedGen2?.subjectBadge)
    }

    // --- Empty Transcripts: Honest Message & Zero Fake Facts ---

    @Test
    fun testEmptyTranscript_honestMessageAndZeroFakeFacts() {
        val result = LectureNotesGenerator.generate(
            transcriptText = "",
            lectureTitle = "Database Normalization"
        )
        val genNotes = result.generatedNotes

        assertEquals("Recorded Lecture Notes", genNotes.lectureTitle)
        assertEquals("General Studies", genNotes.subjectBadge)
        assertTrue(genNotes.aiSummaryText.contains("No spoken content detected"))
        assertTrue(genNotes.keyTakeaways.isNotEmpty())
        assertTrue(genNotes.keyTakeaways[0].contains("No speech or notes could be extracted"))
        assertTrue(genNotes.importantTerms.isEmpty())
        assertTrue(genNotes.detectedQuestionsAndAnswers.isEmpty())
    }
}
