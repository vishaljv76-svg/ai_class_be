package com.example.Simtas.data.ml

import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Question Answering Engine for Voice-to-Text and Lecture Notes.
 *
 * Implements the strict 4-step search hierarchy mandated by the requirements:
 * 0. Mathematical expression calculation via MathExpressionEvaluator
 * 1. Current lecture transcript via TranscriptQaRetriever
 * 2. Current lecture's full generated study notes (Topic-wise explanations, bullets, definitions)
 * 3. Current lecture summary and key takeaways
 * 4. Existing offline knowledge and reasoning engine via OfflineReasoningEvaluator
 *
 * Clearly distinguishes answers from current lecture content vs general knowledge.
 * ZERO paid cloud AI APIs. ZERO arbitrary code execution.
 */
object QuestionAnsweringEngine {

    /**
     * Answers a detected question using the strict 4-tier lecture context search order.
     *
     * @param question The question or prompt to answer.
     * @param transcriptText Spoken lecture transcript text (Search Level 1).
     * @param detailedNotes Current lecture's full generated study notes (Search Level 2).
     * @param summaryText Current lecture summary text (Search Level 3).
     * @param keyTakeaways Current lecture key takeaways (Search Level 3).
     */
    fun answerQuestion(
        question: String,
        transcriptText: String = "",
        detailedNotes: DetailedNotesUiState? = null,
        summaryText: String = "",
        keyTakeaways: List<String> = emptyList()
    ): DetectedQuestionAnswerItem {
        val cleanQ = question.trim()
        if (cleanQ.isEmpty()) {
            return DetectedQuestionAnswerItem(
                question = "",
                answer = "Empty question provided.",
                status = "Invalid",
                isAnswered = false
            )
        }

        // --- Priority 0: Mathematical Question ---
        val mathSolution = MathExpressionEvaluator.evaluate(cleanQ)
        if (mathSolution != null && mathSolution.isAnswered) {
            return mathSolution
        }

        // --- Priority 0.5: Greetings & General Assistant / Identity Queries ---
        val generalAssistantAnswer = evaluateGeneralOrGreeting(cleanQ)
        if (generalAssistantAnswer != null) {
            return generalAssistantAnswer
        }

        // Strip leading greeting prefix if any for lecture search (e.g. "Hello, what is 2NF?" -> "what is 2NF?")
        val strippedGreeting = QuestionDetector.stripGreetingPrefix(cleanQ)
        val effectiveLectureQ = if (strippedGreeting.isNotBlank()) strippedGreeting else cleanQ

        // --- Priority 1: Current Lecture Transcript ---
        if (transcriptText.isNotBlank()) {
            val transcriptAnswer = TranscriptQaRetriever.retrieveAnswer(effectiveLectureQ, transcriptText)
            if (transcriptAnswer != null) {
                return transcriptAnswer
            }
        }

        // --- Priority 2: Current Lecture's Full Generated Study Notes ---
        if (detailedNotes != null) {
            val notesAnswer = searchDetailedStudyNotes(effectiveLectureQ, detailedNotes)
            if (notesAnswer != null) {
                return notesAnswer
            }
        }

        // --- Priority 3: Current Lecture Summary & Key Takeaways ---
        if (summaryText.isNotBlank() || keyTakeaways.isNotEmpty()) {
            val summaryAnswer = searchSummaryAndTakeaways(effectiveLectureQ, summaryText, keyTakeaways)
            if (summaryAnswer != null) {
                return summaryAnswer
            }
        }

        // --- Priority 4: Universal Student Knowledge & Offline Reasoning ---
        // 4a. Programming Assistant (Code Generation, Syntax & Error Debugging)
        val programmingAnswer = ProgrammingAssistantEngine.answer(effectiveLectureQ)
        if (programmingAnswer != null) {
            return programmingAnswer
        }

        // 4b. Universal Student Knowledge Base (Computer Science, Physics, Chemistry, Biology)
        val universalAnswer = UniversalStudentKnowledgeBase.findKnowledge(effectiveLectureQ)
        if (universalAnswer != null) {
            return universalAnswer
        }

        // 4c. Existing Offline Knowledge & Comparative Reasoning Engine
        val generalAnswer = OfflineReasoningEvaluator.evaluate(effectiveLectureQ)
        if (generalAnswer != null) {
            return generalAnswer
        }

        // --- Priority 5: Honest Absentee Indication for Unsupported Questions ---
        return DetectedQuestionAnswerItem(
            question = cleanQ,
            answer = "This question was not covered in the current lecture content.",
            explanation = "No matching explanation was found in the lecture recording or available offline knowledge.",
            status = "Not in lecture",
            isAnswered = false
        )
    }

    /**
     * Evaluates greetings and general identity/assistant inquiries.
     * Answers naturally for greetings ("Hi", "Hello", "How are you?"),
     * and answers "I'm SmartNotes AI, your study assistant." for identity inquiries ("Who are you?", "What are you?").
     */
    fun evaluateGeneralOrGreeting(question: String): DetectedQuestionAnswerItem? {
        val cleanQ = question.trim()
        if (cleanQ.isEmpty()) return null

        val category = QuestionDetector.classify(cleanQ)

        // 1. Identity Inquiry: "Who are you?", "What are you?", "Hi, what are you?", "Hello, who are you?"
        if (category == QuestionDetector.QuestionCategory.GENERAL_IDENTITY) {
            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = "I'm SmartNotes AI, your study assistant.",
                explanation = "SmartNotes AI is an offline study assistant designed to record lectures, transcribe speech, generate notes, and answer academic questions.",
                status = "Answered",
                isAnswered = true
            )
        }

        // 2. Capability Inquiry: "What can you do?", "How do you work?"
        if (category == QuestionDetector.QuestionCategory.GENERAL_ASSISTANT) {
            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = "I can record lectures, transcribe speech offline, generate structured study notes, solve mathematical expressions, and answer your study questions.",
                explanation = "SmartNotes AI provides complete offline speech recognition and study note synthesis.",
                status = "Answered",
                isAnswered = true
            )
        }

        // 3. Greeting Inquiry: "Hi", "Hello", "How are you?", "Hi, how are you?"
        if (category == QuestionDetector.QuestionCategory.GREETING) {
            val lower = cleanQ.lowercase(Locale.ROOT)
                .replace("?", "")
                .replace("!", "")
                .replace(".", "")
                .replace(",", " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

            val answerText = if (lower.contains("how are you") || lower.contains("how are you doing") || lower.contains("hows it going")) {
                if (lower.startsWith("hi") || lower.startsWith("hello") || lower.startsWith("hey")) {
                    "Hello! I'm doing well, thank you! How can I help you with your studies today?"
                } else {
                    "I'm doing well, thank you! Ready to assist you with your lecture notes and study questions."
                }
            } else {
                "Hello! I'm here to help you summarize lectures, create study notes, and answer your academic questions."
            }

            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = answerText,
                explanation = "SmartNotes AI is active and ready to assist with your coursework.",
                status = "Answered",
                isAnswered = true
            )
        }

        return null
    }

    /**
     * Backward-compatible overload accepting simple string context.
     * Maps `lectureContext` across transcript, notes, and general knowledge.
     */
    fun answerQuestion(question: String, lectureContext: String): DetectedQuestionAnswerItem {
        return answerQuestion(
            question = question,
            transcriptText = lectureContext
        )
    }

    /**
     * Searches the lecture's full detailed study notes for an answer (Search Level 2).
     */
    private fun searchDetailedStudyNotes(
        question: String,
        notes: DetailedNotesUiState
    ): DetectedQuestionAnswerItem? {
        val cleanLower = cleanQuery(question)
        if (cleanLower.isBlank()) return null
        val queryWords = cleanLower.split(" ").filter { it.isNotBlank() && it.length >= 2 }
        if (queryWords.isEmpty()) return null

        // 1. Check Section 2 Topic-Wise Bullets (Highest priority in study notes)
        // Pass 1: Look for specific leading definition / title (e.g. starts with "2NF" or contains "(2NF)")
        for (bullet in notes.section2Bullets) {
            val bulletLower = bullet.lowercase(Locale.ROOT)
            val isSpecificMatch = bulletLower.startsWith(cleanLower) ||
                bulletLower.contains("($cleanLower)") ||
                (cleanLower == "1nf" && (bulletLower.contains("first normal form") || bulletLower.startsWith("1nf"))) ||
                (cleanLower == "2nf" && (bulletLower.contains("second normal form") || bulletLower.startsWith("2nf"))) ||
                (cleanLower == "3nf" && (bulletLower.contains("third normal form") || bulletLower.startsWith("3nf")))

            if (isSpecificMatch && !bulletLower.contains("1nf, 2nf, 3nf")) {
                return DetectedQuestionAnswerItem(
                    question = question,
                    answer = bullet,
                    explanation = "Extracted directly from the lecture study notes.",
                    status = "Answered from Study Notes",
                    isAnswered = true
                )
            }
        }

        // Pass 2: General keyword match
        for (bullet in notes.section2Bullets) {
            val bulletLower = bullet.lowercase(Locale.ROOT)
            if (bulletLower.contains("1nf, 2nf, 3nf") && (cleanLower == "1nf" || cleanLower == "2nf" || cleanLower == "3nf")) {
                continue // Skip general list when searching for a specific normal form
            }
            if (bulletLower.contains(cleanLower) || (queryWords.size == 1 && bulletLower.contains(queryWords[0]))) {
                return DetectedQuestionAnswerItem(
                    question = question,
                    answer = bullet,
                    explanation = "Extracted directly from the lecture study notes.",
                    status = "Answered from Study Notes",
                    isAnswered = true
                )
            }
            // Multi-word overlap match
            var matchCount = 0
            for (w in queryWords) {
                if (bulletLower.contains(w)) matchCount++
            }
            if (matchCount >= (queryWords.size.coerceAtMost(2))) {
                return DetectedQuestionAnswerItem(
                    question = question,
                    answer = bullet,
                    explanation = "Extracted directly from the lecture study notes.",
                    status = "Answered from Study Notes",
                    isAnswered = true
                )
            }
        }

        // 2. Check Callout definition / formula
        val calloutText = "${notes.calloutTitle}: ${notes.calloutContent}"
        val calloutLower = calloutText.lowercase(Locale.ROOT)
        if (calloutLower.contains(cleanLower) || queryWords.all { calloutLower.contains(it) }) {
            return DetectedQuestionAnswerItem(
                question = question,
                answer = notes.calloutContent,
                explanation = "Extracted from lecture key principle: ${notes.calloutTitle}.",
                status = "Answered from Study Notes",
                isAnswered = true
            )
        }

        // 3. Check Section 1 Overview
        val sec1Lower = notes.section1Content.lowercase(Locale.ROOT)
        if (sec1Lower.contains(cleanLower) || queryWords.all { sec1Lower.contains(it) }) {
            return DetectedQuestionAnswerItem(
                question = question,
                answer = notes.section1Content,
                explanation = "Extracted directly from the lecture study notes overview.",
                status = "Answered from Study Notes",
                isAnswered = true
            )
        }

        // 4. Check Section 3 Discussions & Worked Examples
        val sec3Text = "${notes.section3Content1} ${notes.section3Content2}"
        val sec3Lower = sec3Text.lowercase(Locale.ROOT)
        if (sec3Lower.contains(cleanLower) || queryWords.all { sec3Lower.contains(it) }) {
            val answerText = if (notes.section3Content1.isNotBlank()) notes.section3Content1 else notes.section3Content2
            return DetectedQuestionAnswerItem(
                question = question,
                answer = answerText,
                explanation = "Extracted directly from the lecture study notes discussion.",
                status = "Answered from Study Notes",
                isAnswered = true
            )
        }

        return null
    }

    /**
     * Searches lecture summary and key takeaways for an answer (Search Level 3).
     */
    private fun searchSummaryAndTakeaways(
        question: String,
        summaryText: String,
        takeaways: List<String>
    ): DetectedQuestionAnswerItem? {
        val cleanLower = cleanQuery(question)
        if (cleanLower.isBlank()) return null
        val queryWords = cleanLower.split(" ").filter { it.isNotBlank() && it.length >= 2 }
        if (queryWords.isEmpty()) return null

        // 1. Search key takeaways
        for (takeaway in takeaways) {
            val takeawayLower = takeaway.lowercase(Locale.ROOT)
            if (takeawayLower.contains(cleanLower) || (queryWords.size == 1 && takeawayLower.contains(queryWords[0]))) {
                return DetectedQuestionAnswerItem(
                    question = question,
                    answer = takeaway,
                    explanation = "Extracted from lecture summary and key takeaways.",
                    status = "Answered from Summary & Takeaways",
                    isAnswered = true
                )
            }
            var matches = 0
            for (w in queryWords) {
                if (takeawayLower.contains(w)) matches++
            }
            if (matches >= (queryWords.size.coerceAtMost(2))) {
                return DetectedQuestionAnswerItem(
                    question = question,
                    answer = takeaway,
                    explanation = "Extracted from lecture summary and key takeaways.",
                    status = "Answered from Summary & Takeaways",
                    isAnswered = true
                )
            }
        }

        // 2. Search summary text sentences
        if (summaryText.isNotBlank()) {
            val sentences = summaryText.split(Regex("""(?<=[.!?])\s+|\n+""")).map { it.trim() }
            for (sentence in sentences) {
                val sentLower = sentence.lowercase(Locale.ROOT)
                if (sentLower.contains(cleanLower) || queryWords.all { sentLower.contains(it) }) {
                    return DetectedQuestionAnswerItem(
                        question = question,
                        answer = sentence,
                        explanation = "Extracted from lecture summary and key takeaways.",
                        status = "Answered from Summary & Takeaways",
                        isAnswered = true
                    )
                }
            }
        }

        return null
    }

    private fun cleanQuery(q: String): String {
        return q.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace(",", " ")
            .replace(
                Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""),
                ""
            )
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
