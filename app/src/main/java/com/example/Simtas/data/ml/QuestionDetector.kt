package com.example.Simtas.data.ml

import java.util.Locale

/**
 * High-precision Question Detection Component.
 * Identifies actual questions, directive queries, and mathematical expressions
 * from spoken or typed lecture text, while strictly ignoring ordinary statements.
 */
object QuestionDetector {

    // Directive prefixes that command an answer or explanation
    private val DIRECTIVE_PREFIXES = listOf(
        "explain", "define", "describe", "clarify", "differentiate",
        "compare", "calculate", "evaluate", "solve", "compute", "summarize",
        "tell me about", "tell me what is", "tell me how", "can you explain",
        "could you explain", "please explain", "can you tell me",
        "name", "list", "give", "mention", "state", "identify", "provide"
    )

    // Standard interrogative question words
    private val QUESTION_WORDS = listOf(
        "what", "why", "how", "when", "where", "who", "whom", "which"
    )

    // Inversion auxiliary verbs that begin inquiry questions
    private val INVERSION_WORDS = listOf(
        "is", "are", "do", "does", "did", "can", "could", "would", "should", "will"
    )

    /**
     * Determines whether the given text represents a question or mathematical expression.
     * Returns false for ordinary lecture statements (e.g. "Database normalization reduces data redundancy.").
     */
    fun isQuestion(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        // 1. Standalone mathematical expression (e.g. "2+2", "10*5", "sqrt(64)")
        if (MathExpressionSolver.hasMathExpression(trimmed)) {
            return true
        }

        // 2. Explicit question mark
        if (trimmed.endsWith("?")) {
            return true
        }

        val lower = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("""^[0-9]+[.\-)]\s*"""), "") // remove leading numbering (e.g. "1. ")
            .replace(Regex("""^speaker\s*\d*\s*:\s*"""), "") // remove speaker prefix
            .replace(",", " ") // handle speech recognition commas like "Name, some" -> "Name some"
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 3. Directive commands ("explain ...", "define ...", "name ...", "calculate ...")
        for (prefix in DIRECTIVE_PREFIXES) {
            if (lower == prefix || lower.startsWith("$prefix ") || lower.startsWith("$prefix\t")) {
                if (lower.length > prefix.length + 2) {
                    return true
                }
            }
        }

        // 4. Starts with a standard question word ("what", "why", "how", etc.)
        val words = lower.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val firstWord = words.firstOrNull() ?: ""

        if (firstWord in QUESTION_WORDS) {
            return true
        }

        // 5. Inversion question (e.g. "Is normalization a process?", "Can machines think?")
        if (firstWord in INVERSION_WORDS && words.size >= 3) {
            return true
        }

        // 6. Ordinary lecture statement (e.g. "Database normalization reduces data redundancy.")
        return false
    }

    /**
     * Extracts individual questions from a multi-sentence or multiline text block.
     * Rejects ordinary statements and removes duplicates.
     */
    fun extractQuestions(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // If entire input is a standalone math expression without newlines (e.g. "2+2")
        if (MathExpressionSolver.hasMathExpression(trimmed) && !trimmed.contains("\n")) {
            return listOf(trimmed)
        }

        val candidates = mutableListOf<String>()

        // Split by lines first
        val lines = trimmed.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            // Split line into sentences by question marks or sentence-ending periods
            val parts = line.split(Regex("""(?<=[?])\s+|(?<=[.!])\s+(?=[A-Z0-9])"""))
            for (part in parts) {
                val cleanPart = part.trim()
                if (isQuestion(cleanPart)) {
                    candidates.add(cleanPart)
                }
            }
        }

        return candidates.distinct()
    }
}

