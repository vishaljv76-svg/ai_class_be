package com.example.Simtas

import com.example.Simtas.data.local.LectureNotesStorage
import com.example.Simtas.data.ml.MathExpressionEvaluator
import com.example.Simtas.data.ml.MathExpressionSolver
import com.example.Simtas.data.ml.ProgrammingAssistantEngine
import com.example.Simtas.data.ml.QuestionAnsweringEngine
import com.example.Simtas.data.ml.QuestionDetector
import com.example.Simtas.data.ml.UniversalStudentKnowledgeBase
import com.example.Simtas.data.remote.SessionManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Universal Student Question Answering Engine Test Suite.
 * Comprehensive tests covering:
 * 1. General Questions & Greetings
 * 2. Mathematics (Arithmetic, Percentages, Fractions, Algebra/Equations, Powers, Roots)
 * 3. Computer Science (Java, Kotlin, Python, C, C++, Data Structures, Algorithms, Databases, SQL, OS, Networks)
 * 4. Science (Physics, Chemistry, Biology)
 * 5. Educational Questions (Definitions, Comparisons, Explanations)
 * 6. Programming Assistant (Code Examples, Error Debugging)
 * 7. Lecture Context Priority (Transcript > Notes > Summary > Knowledge Base > Absentee)
 * 8. Question Classification (All 9 Categories)
 * 9. Answer Quality & Distinct Sources
 */
class UniversalQuestionAnsweringTest {

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

    // ==========================================
    // 1. GENERAL QUESTIONS & GREETINGS
    // ==========================================

    @Test
    fun testGeneralGreetings_respondNaturally() {
        val greetings = listOf("Hi", "Hello", "How are you?", "Hi, how are you?", "Good morning")
        for (g in greetings) {
            val result = QuestionAnsweringEngine.answerQuestion(g)
            assertTrue("'$g' must be answered", result.isAnswered)
            assertTrue("Status must be Answered", result.status.contains("Answered"))
            assertFalse("Must not claim not covered", result.answer.contains("not covered in the current lecture"))
        }

        val whoAreYou = QuestionAnsweringEngine.answerQuestion("Who are you?")
        assertEquals("I'm SmartNotes AI, your study assistant.", whoAreYou.answer)
        assertTrue(whoAreYou.isAnswered)

        val whatAreYou = QuestionAnsweringEngine.answerQuestion("What are you?")
        assertEquals("I'm SmartNotes AI, your study assistant.", whatAreYou.answer)
        assertTrue(whatAreYou.isAnswered)

        val whatCanYouDo = QuestionAnsweringEngine.answerQuestion("What can you do?")
        assertTrue(whatCanYouDo.isAnswered)
        assertTrue(whatCanYouDo.answer.contains("record lectures", ignoreCase = true))
    }

    // ==========================================
    // 2. MATHEMATICS (Arithmetic, Fractions, Powers, Roots, Algebra)
    // ==========================================

    @Test
    fun testMath_BasicArithmetic() {
        val resAdd = QuestionAnsweringEngine.answerQuestion("What is 25 + 75?")
        assertTrue(resAdd.isAnswered)
        assertEquals("100", resAdd.answer)

        val resMul = QuestionAnsweringEngine.answerQuestion("12 * 8")
        assertTrue(resMul.isAnswered)
        assertEquals("96", resMul.answer)

        val resDiv = QuestionAnsweringEngine.answerQuestion("100 / 4")
        assertTrue(resDiv.isAnswered)
        assertEquals("25", resDiv.answer)
    }

    @Test
    fun testMath_PowersAndRoots() {
        // Powers
        val resPow = QuestionAnsweringEngine.answerQuestion("What is 2 to the power of 5?")
        assertTrue(resPow.isAnswered)
        assertEquals("32", resPow.answer)

        val resSquare = QuestionAnsweringEngine.answerQuestion("5 squared")
        assertTrue(resSquare.isAnswered)
        assertEquals("25", resSquare.answer)

        // Square root
        val resSqrt = QuestionAnsweringEngine.answerQuestion("Square root of 144")
        assertTrue(resSqrt.isAnswered)
        assertEquals("12", resSqrt.answer)

        // Cube root
        val resCbrt = QuestionAnsweringEngine.answerQuestion("Cube root of 27")
        assertTrue(resCbrt.isAnswered)
        assertEquals("3", resCbrt.answer)
    }

    @Test
    fun testMath_PercentagesAndFractions() {
        // Percentage
        val resPct = QuestionAnsweringEngine.answerQuestion("What is 20% of 200?")
        assertTrue(resPct.isAnswered)
        assertEquals("40", resPct.answer)

        // Fraction calculation
        val resFrac = QuestionAnsweringEngine.answerQuestion("1/2 + 1/4")
        assertTrue(resFrac.isAnswered)
        assertEquals("0.75", resFrac.answer)
    }

    @Test
    fun testMath_LinearEquationsWithSteps() {
        // Solve for x: 2x + 6 = 14
        val resEq = QuestionAnsweringEngine.answerQuestion("Solve for x: 2x + 6 = 14")
        assertTrue(resEq.isAnswered)
        assertEquals("x = 4", resEq.answer)
        assertTrue("Must include step-by-step reasoning", resEq.explanation.contains("Step 1", ignoreCase = true))
        assertTrue("Must show operations", resEq.explanation.contains("Subtract 6", ignoreCase = true))

        // Solve x + 5 = 12
        val resEq2 = QuestionAnsweringEngine.answerQuestion("Solve x + 5 = 12")
        assertTrue(resEq2.isAnswered)
        assertEquals("x = 7", resEq2.answer)

        // 3x = 15
        val resEq3 = QuestionAnsweringEngine.answerQuestion("3x = 15")
        assertTrue(resEq3.isAnswered)
        assertEquals("x = 5", resEq3.answer)
    }

    // ==========================================
    // 3. COMPUTER SCIENCE KNOWLEDGE BASE
    // ==========================================

    @Test
    fun testComputerScience_ProgrammingLanguages() {
        // Java
        val resJava = QuestionAnsweringEngine.answerQuestion("What is Java?")
        assertTrue(resJava.isAnswered)
        assertTrue(resJava.answer.contains("JVM", ignoreCase = true))
        assertTrue(resJava.status.contains("Computer Science"))

        // Kotlin
        val resKotlin = QuestionAnsweringEngine.answerQuestion("What is Kotlin?")
        assertTrue(resKotlin.isAnswered)
        assertTrue(resKotlin.answer.contains("null safety", ignoreCase = true))

        // Python
        val resPython = QuestionAnsweringEngine.answerQuestion("Explain Python programming language")
        assertTrue(resPython.isAnswered)
        assertTrue(resPython.answer.contains("readability", ignoreCase = true))

        // C & C++
        val resC = QuestionAnsweringEngine.answerQuestion("What is C language?")
        assertTrue(resC.isAnswered)
        assertTrue(resC.answer.contains("pointers", ignoreCase = true))

        val resCpp = QuestionAnsweringEngine.answerQuestion("What is C++?")
        assertTrue(resCpp.isAnswered)
        assertTrue(resCpp.answer.contains("STL", ignoreCase = true) || resCpp.answer.contains("classes", ignoreCase = true))
    }

    @Test
    fun testComputerScience_DataStructuresAndAlgorithms() {
        // Data Structures
        val resArray = QuestionAnsweringEngine.answerQuestion("What is an array?")
        assertTrue(resArray.isAnswered)
        assertTrue(resArray.answer.contains("contiguous", ignoreCase = true))

        val resStack = QuestionAnsweringEngine.answerQuestion("What is a stack?")
        assertTrue(resStack.isAnswered)
        assertTrue(resStack.answer.contains("LIFO", ignoreCase = true))

        val resQueue = QuestionAnsweringEngine.answerQuestion("What is a queue?")
        assertTrue(resQueue.isAnswered)
        assertTrue(resQueue.answer.contains("FIFO", ignoreCase = true))

        // Algorithms
        val resBinarySearch = QuestionAnsweringEngine.answerQuestion("How does binary search work?")
        assertTrue(resBinarySearch.isAnswered)
        assertTrue(resBinarySearch.answer.contains("sorted", ignoreCase = true))
        assertTrue(resBinarySearch.answer.contains("O(log n)", ignoreCase = true))

        val resBigO = QuestionAnsweringEngine.answerQuestion("What is Big O notation?")
        assertTrue(resBigO.isAnswered)
        assertTrue(resBigO.answer.contains("complexity", ignoreCase = true))
    }

    @Test
    fun testComputerScience_DatabasesAndOperatingSystems() {
        // Databases & ACID
        val resAcid = QuestionAnsweringEngine.answerQuestion("What are ACID properties in databases?")
        assertTrue(resAcid.isAnswered)
        assertTrue(resAcid.answer.contains("Atomicity", ignoreCase = true))
        assertTrue(resAcid.answer.contains("Consistency", ignoreCase = true))
        assertTrue(resAcid.answer.contains("Isolation", ignoreCase = true))
        assertTrue(resAcid.answer.contains("Durability", ignoreCase = true))

        val resJoins = QuestionAnsweringEngine.answerQuestion("Explain SQL joins")
        assertTrue(resJoins.isAnswered)
        assertTrue(resJoins.answer.contains("INNER JOIN", ignoreCase = true))

        // Operating Systems
        val resDeadlock = QuestionAnsweringEngine.answerQuestion("What is deadlock in operating systems?")
        assertTrue(resDeadlock.isAnswered)
        assertTrue(resDeadlock.answer.contains("Mutual Exclusion", ignoreCase = true) || resDeadlock.answer.contains("blocked", ignoreCase = true))

        val resVirtualMem = QuestionAnsweringEngine.answerQuestion("What is virtual memory?")
        assertTrue(resVirtualMem.isAnswered)
        assertTrue(resVirtualMem.answer.contains("Paging", ignoreCase = true) || resVirtualMem.answer.contains("RAM", ignoreCase = true))
    }

    @Test
    fun testComputerScience_Networks() {
        val resOsi = QuestionAnsweringEngine.answerQuestion("What is the OSI model?")
        assertTrue(resOsi.isAnswered)
        assertTrue(resOsi.answer.contains("Application", ignoreCase = true))
        assertTrue(resOsi.answer.contains("Transport", ignoreCase = true))

        val resTcpUdp = QuestionAnsweringEngine.answerQuestion("What is the difference between TCP and UDP?")
        assertTrue(resTcpUdp.isAnswered)
        assertTrue(resTcpUdp.answer.contains("connection-oriented", ignoreCase = true))
        assertTrue(resTcpUdp.answer.contains("connectionless", ignoreCase = true))
    }

    // ==========================================
    // 4. SCIENCE (Physics, Chemistry, Biology)
    // ==========================================

    @Test
    fun testScience_Physics() {
        val resNewton = QuestionAnsweringEngine.answerQuestion("What is Newton's second law?")
        assertTrue(resNewton.isAnswered)
        assertTrue(resNewton.answer.contains("F = ma", ignoreCase = true))
        assertTrue(resNewton.status.contains("Science"))

        val resOhm = QuestionAnsweringEngine.answerQuestion("What is Ohm's law?")
        assertTrue(resOhm.isAnswered)
        assertTrue(resOhm.answer.contains("V = I", ignoreCase = true))

        val resGravity = QuestionAnsweringEngine.answerQuestion("What is gravity?")
        assertTrue(resGravity.isAnswered)
        assertTrue(resGravity.answer.contains("attraction", ignoreCase = true))
    }

    @Test
    fun testScience_Chemistry() {
        val resAtom = QuestionAnsweringEngine.answerQuestion("What is an atom?")
        assertTrue(resAtom.isAnswered)
        assertTrue(resAtom.answer.contains("protons", ignoreCase = true))
        assertTrue(resAtom.answer.contains("electrons", ignoreCase = true))

        val resBonds = QuestionAnsweringEngine.answerQuestion("What are chemical bonds?")
        assertTrue(resBonds.isAnswered)
        assertTrue(resBonds.answer.contains("Covalent", ignoreCase = true))
        assertTrue(resBonds.answer.contains("Ionic", ignoreCase = true))

        val resAcids = QuestionAnsweringEngine.answerQuestion("What are acids and bases?")
        assertTrue(resAcids.isAnswered)
        assertTrue(resAcids.answer.contains("pH", ignoreCase = true))
    }

    @Test
    fun testScience_Biology() {
        val resPhoto = QuestionAnsweringEngine.answerQuestion("What is photosynthesis?")
        assertTrue(resPhoto.isAnswered)
        assertTrue(resPhoto.answer.contains("glucose", ignoreCase = true))
        assertTrue(resPhoto.answer.contains("chloroplast", ignoreCase = true) || resPhoto.answer.contains("chlorophyll", ignoreCase = true))

        val resResp = QuestionAnsweringEngine.answerQuestion("What is cellular respiration?")
        assertTrue(resResp.isAnswered)
        assertTrue(resResp.answer.contains("ATP", ignoreCase = true))

        val resDnaRna = QuestionAnsweringEngine.answerQuestion("What is the difference between DNA and RNA?")
        assertTrue(resDnaRna.isAnswered)
        assertTrue(resDnaRna.answer.contains("deoxyribose", ignoreCase = true) || resDnaRna.answer.contains("Thymine", ignoreCase = true))
        assertTrue(resDnaRna.answer.contains("Uracil", ignoreCase = true) || resDnaRna.answer.contains("ribose", ignoreCase = true))
    }

    // ==========================================
    // 5. PROGRAMMING ASSISTANT (Code & Bug Debugging)
    // ==========================================

    @Test
    fun testProgrammingAssistant_CodeGeneration() {
        val resPyRev = QuestionAnsweringEngine.answerQuestion("How to reverse a string in Python")
        assertTrue(resPyRev.isAnswered)
        assertTrue(resPyRev.status.contains("Programming Assistant"))
        assertTrue(resPyRev.answer.contains("[::-1]"))

        val resPyBin = QuestionAnsweringEngine.answerQuestion("Show binary search in Python")
        assertTrue(resPyBin.isAnswered)
        assertTrue(resPyBin.answer.contains("def binary_search"))

        val resCPtr = QuestionAnsweringEngine.answerQuestion("Pointers in C")
        assertTrue(resCPtr.isAnswered)
        assertTrue(resCPtr.answer.contains("#include <stdio.h>"))
        assertTrue(resCPtr.answer.contains("*ptr"))
    }

    @Test
    fun testProgrammingAssistant_BugDebugging() {
        val resNpe = QuestionAnsweringEngine.answerQuestion("What is NullPointerException and how to fix it?")
        assertTrue(resNpe.isAnswered)
        assertTrue(resNpe.status.contains("Programming Assistant"))
        assertTrue(resNpe.answer.contains("Cause:", ignoreCase = true))
        assertTrue(resNpe.answer.contains("How to Fix:", ignoreCase = true))

        val resSegFault = QuestionAnsweringEngine.answerQuestion("What is a segmentation fault?")
        assertTrue(resSegFault.isAnswered)
        assertTrue(resSegFault.answer.contains("memory", ignoreCase = true))
    }

    // ==========================================
    // 6. LECTURE CONTEXT PRIORITY
    // ==========================================

    @Test
    fun testLectureContextPriority_OverridesGeneralKnowledge() {
        val transcript = "Database normalization in this lecture focuses specifically on Boyce-Codd Normal Form (BCNF). " +
                "BCNF requires that for every functional dependency X -> Y, X must be a superkey."

        // Ask about BCNF with lecture context
        val result = QuestionAnsweringEngine.answerQuestion(
            question = "What is BCNF?",
            transcriptText = transcript
        )
        assertTrue(result.isAnswered)
        // Must prioritize transcript
        assertEquals("Answered from Transcript", result.status)
        assertTrue(result.answer.contains("superkey", ignoreCase = true))
    }

    // ==========================================
    // 7. QUESTION CLASSIFICATION (All 9 Categories)
    // ==========================================

    @Test
    fun testQuestionClassification_AllCategories() {
        assertEquals(QuestionDetector.QuestionCategory.GREETING, QuestionDetector.classifyCategory("Hello, how are you?"))
        assertEquals(QuestionDetector.QuestionCategory.GENERAL_KNOWLEDGE, QuestionDetector.classifyCategory("Who are you?"))
        assertEquals(QuestionDetector.QuestionCategory.MATHEMATICS, QuestionDetector.classifyCategory("Solve for x: 2x + 6 = 14"))
        assertEquals(QuestionDetector.QuestionCategory.MATHEMATICS, QuestionDetector.classifyCategory("2 + 2"))
        assertEquals(QuestionDetector.QuestionCategory.PROGRAMMING, QuestionDetector.classifyCategory("How to reverse a string in Python"))
        assertEquals(QuestionDetector.QuestionCategory.PROGRAMMING, QuestionDetector.classifyCategory("What is an array in Java?"))
        assertEquals(QuestionDetector.QuestionCategory.SCIENCE, QuestionDetector.classifyCategory("What is photosynthesis in biology?"))
        assertEquals(QuestionDetector.QuestionCategory.SCIENCE, QuestionDetector.classifyCategory("What is Newton's second law in physics?"))
        assertEquals(QuestionDetector.QuestionCategory.COMPARISON, QuestionDetector.classifyCategory("What is the difference between TCP and UDP?"))
        assertEquals(QuestionDetector.QuestionCategory.DEFINITION, QuestionDetector.classifyCategory("Define encapsulation"))
        assertEquals(QuestionDetector.QuestionCategory.UNSUPPORTED, QuestionDetector.classifyCategory("Today is a sunny morning for a walk in the park."))
    }

    // ==========================================
    // 8. UNSUPPORTED QUESTIONS HONEST ABSENTEE
    // ==========================================

    @Test
    fun testUnsupportedQuestion_ReturnsHonestMessage() {
        val unsupportedQuestion = "Who was Julius Caesar and when was he born?"
        val result = QuestionAnsweringEngine.answerQuestion(unsupportedQuestion)
        assertFalse("Unsupported question must not claim to be answered", result.isAnswered)
        assertEquals("Not in lecture", result.status)
        assertTrue(result.answer.contains("This question was not covered in the current lecture content"))
    }
}

