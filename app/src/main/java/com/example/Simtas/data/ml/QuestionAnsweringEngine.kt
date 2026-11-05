package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Question Answering Engine for Voice-to-Text and Lecture Notes.
 *
 * Implements strict hierarchical answer generation:
 * 1. Mathematical calculation via safe local parser (MathExpressionSolver)
 * 2. Grounded lecture answers extracted from actual transcript/notes
 * 3. Verified foundational educational curriculum concepts
 * 4. Clear and honest indication when information is absent from the lecture
 *
 * ZERO paid cloud AI APIs. ZERO arbitrary code execution.
 */
object QuestionAnsweringEngine {

    // Verified foundational educational curriculum definitions
    private val CURRICULUM_KNOWLEDGE_BASE = mapOf(
        "domestic and wild animals" to "Domestic animals are animals kept, raised, or tamed by humans for companionship or agriculture (e.g., dogs, cats, cows, sheep, horses). Wild animals live independently in natural habitats without human care or domestication (e.g., lions, tigers, elephants, bears, deer).",
        "domestic animals" to "Domestic animals are animals that have been selectively bred and adapted over generations to live alongside humans for companionship, labor, or agriculture (e.g., dogs, cats, cows, horses, sheep).",
        "wild animals" to "Wild animals are animals that live and reproduce independently in natural ecosystems such as forests, plains, and oceans without human management (e.g., lions, tigers, elephants, bears, wolves).",
        "database normalization" to "Database normalization is the process of organizing data in a relational database to reduce redundancy and improve data integrity.",
        "normalization" to "Database normalization is the process of structuring relational tables to eliminate insertion, update, and deletion anomalies while ensuring dependency preservation.",
        "artificial intelligence" to "Artificial intelligence is the simulation of human intelligence processes by computer systems, encompassing learning, reasoning, and self-correction.",
        "machine learning" to "Machine learning is a branch of artificial intelligence focused on building applications that learn from data and improve accuracy over time without being explicitly programmed.",
        "deep learning" to "Deep learning is a subset of machine learning based on artificial neural networks with multiple layers to model complex patterns in data.",
        "photosynthesis" to "Photosynthesis is the biological process used by green plants and algae to convert light energy into chemical energy stored in glucose.",
        "cell" to "A cell is the fundamental structural, functional, and biological unit of all living organisms.",
        "dna" to "DNA (deoxyribonucleic acid) is the hereditary molecule that carries genetic instructions for development, functioning, growth, and reproduction in all organisms.",
        "ecosystem" to "An ecosystem is a biological community of interacting organisms and their physical environment functioning together.",
        "gravity" to "Gravity is the fundamental physical force of attraction between all objects with mass or energy throughout the universe.",
        "newton's laws" to "Newton's laws of motion are three physical laws that describe the relationship between the motion of an object and the forces acting upon it.",
        "atom" to "An atom is the basic building block of matter, composed of protons, neutrons, and electrons.",
        "algorithm" to "An algorithm is a finite sequence of unambiguous instructions used to solve a specific problem or perform a computation.",
        "data structure" to "A data structure is a specialized format for organizing, processing, retrieving, and storing data efficiently in memory.",
        "relational database" to "A relational database is a digital database based on the relational model of data, organizing data into tables consisting of rows and columns.",
        "functional dependency" to "Functional dependency is a constraint between two attributes or sets of attributes in a relational database where one uniquely determines another.",
        "normal forms" to "Normal forms (1NF, 2NF, 3NF, BCNF) are progressive stages of database normalization used to eliminate specific types of data redundancies.",
        "operating system" to "An operating system is system software that manages computer hardware, software resources, and provides common services for computer programs.",
        "cache" to "A cache is a high-speed hardware or software storage layer that stores a subset of data so that future requests for that data are served faster."
    )

    /**
     * Answers a detected question using the current lecture context.
     *
     * @param question The question string.
     * @param lectureContext Full transcript or lecture notes text to ground answers.
     */
    fun answerQuestion(
        question: String,
        lectureContext: String = ""
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

        // --- Priority A: Mathematical Question ---
        val mathSolution = MathExpressionSolver.solveTranscript(cleanQ)
        if (mathSolution != null && mathSolution.isSuccess) {
            return DetectedQuestionAnswerItem(
                question = mathSolution.question,
                answer = mathSolution.answer,
                explanation = mathSolution.explanation,
                status = "Answered",
                isAnswered = true
            )
        }

        // --- Priority B: Grounded in Actual Lecture Content ---
        if (lectureContext.isNotBlank()) {
            val lectureAnswer = extractAnswerFromLecture(cleanQ, lectureContext)
            if (lectureAnswer != null) {
                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = lectureAnswer,
                    explanation = "Extracted directly from the lecture content.",
                    status = "Answered from Lecture",
                    isAnswered = true
                )
            }
        }

        // --- Priority C: Foundational Educational Knowledge ---
        val generalAnswer = lookupCurriculumKnowledge(cleanQ)
        if (generalAnswer != null) {
            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = generalAnswer,
                explanation = "Verified curriculum educational definition.",
                status = "Answered",
                isAnswered = true
            )
        }

        // --- Priority D: Honest Absentee Indication ---
        return DetectedQuestionAnswerItem(
            question = cleanQ,
            answer = "This question was not covered in the current lecture content.",
            explanation = "No matching explanation was found in the recording or transcript.",
            status = "Not in lecture",
            isAnswered = false
        )
    }

    /**
     * Extracts an answer sentence grounded in the provided lecture transcript or notes.
     */
    private fun extractAnswerFromLecture(question: String, lectureText: String): String? {
        val cleanLower = question.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace(",", " ")
            .replace(Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanLower.isBlank()) return null

        val queryKeywords = cleanLower.split("\\s+".toRegex())
            .filter { it.length > 2 && it !in setOf("the", "and", "for", "with", "that", "this", "from", "some") }

        if (queryKeywords.isEmpty()) return null

        // Split lecture into distinct sentences
        val sentences = lectureText.split(Regex("""(?<=[.!?])\s+|\n+"""))
            .map { it.trim() }
            .filter { it.length >= 10 }

        var bestSentence: String? = null
        var bestScore = 0

        for (sentence in sentences) {
            val sentenceLower = sentence.lowercase(Locale.ROOT)

            // A sentence cannot be an explanatory answer if it is a question or identical to the query
            if (QuestionDetector.isQuestion(sentence)) continue
            if (sentence.equals(question, ignoreCase = true)) continue
            val sentenceClean = sentenceLower.replace(Regex("""[^a-z0-9]"""), "")
            val queryClean = question.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
            if (sentenceClean == queryClean) continue

            // Direct definition match: e.g. "X is defined as Y" or "X is the process of Y"
            if (sentenceLower.contains(cleanLower) && (sentenceLower.contains(" is ") || sentenceLower.contains(" means ") || sentenceLower.contains(" refers to "))) {
                return sentence
            }

            // Keyword overlap score
            var score = 0
            for (kw in queryKeywords) {
                if (sentenceLower.contains(kw)) {
                    score++
                }
            }

            if (score > bestScore && score >= (queryKeywords.size.coerceAtMost(2))) {
                bestScore = score
                bestSentence = sentence
            }
        }

        return bestSentence
    }

    /**
     * Looks up verified foundational curriculum definitions.
     */
    private fun lookupCurriculumKnowledge(question: String): String? {
        val lower = question.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace(",", " ")
            .replace(Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 1. Direct key match
        CURRICULUM_KNOWLEDGE_BASE[lower]?.let { return it }

        // 2. Substring match
        for ((key, definition) in CURRICULUM_KNOWLEDGE_BASE) {
            if (lower.contains(key) || key.contains(lower)) {
                return definition
            }
        }

        // 3. Keyword multi-term match
        val words = lower.split(" ").filter { it.length > 3 && it != "some" }
        if (words.size >= 2) {
            for ((key, definition) in CURRICULUM_KNOWLEDGE_BASE) {
                val keyWords = key.split(" ").filter { it.length > 3 }
                if (keyWords.isNotEmpty() && keyWords.all { lower.contains(it) }) {
                    return definition
                }
            }
        }

        return null
    }
}

