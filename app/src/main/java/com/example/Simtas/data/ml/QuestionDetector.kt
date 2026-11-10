package com.example.Simtas.data.ml

import java.util.Locale

/**
 * High-precision Question Detection Component.
 * Identifies actual questions, directive queries, greetings, general identity queries,
 * and mathematical expressions from spoken or typed lecture text, while strictly
 * ignoring ordinary lecture statements.
 */
object QuestionDetector {

    /**
     * Categories of student inquiries detected by SmartNotes AI (Requirement 8).
     */
    enum class QuestionCategory {
        GREETING,
        GENERAL_KNOWLEDGE,
        MATHEMATICS,
        MATHEMATICAL,
        PROGRAMMING,
        SCIENCE,
        LECTURE_RELATED,
        COMPARISON,
        DEFINITION,
        UNSUPPORTED,

        // Additional sub-categories
        GENERAL_IDENTITY,
        GENERAL_ASSISTANT,
        STATEMENT
    }

    // Standalone greetings
    private val STANDALONE_GREETINGS = setOf(
        "hi", "hello", "hey", "howdy", "greetings",
        "good morning", "good afternoon", "good evening", "good day",
        "how are you", "how are you doing", "hows it going", "how's it going",
        "how are things", "how do you do",
        "வணக்கம்", "வணக்கம் எப்படி இருக்கிறீர்கள்", "நலமா"
    )

    // Identity inquiry patterns ("Who are you?", "What are you?", "Hi, what are you?")
    private val IDENTITY_PATTERNS = listOf(
        "who are you", "what are you", "who r u", "what r u",
        "tell me who you are", "tell me what you are",
        "what is your name", "who made you", "who created you",
        "introduce yourself", "நீங்கள் யார்", "உன் பெயர் என்ன"
    )

    // General assistant capability inquiry patterns
    private val CAPABILITY_PATTERNS = listOf(
        "what can you do", "what do you do", "how can you help",
        "how do you work", "help me", "what is your purpose", "how to use",
        "உங்களால் என்ன செய்ய முடியும்"
    )

    // Directive prefixes that command an answer or explanation
    private val DIRECTIVE_PREFIXES = listOf(
        "explain", "define", "describe", "clarify", "differentiate",
        "compare", "calculate", "evaluate", "solve", "compute", "summarize",
        "tell me about", "tell me what is", "tell me how", "can you explain",
        "could you explain", "please explain", "can you tell me",
        "name", "list", "give", "mention", "state", "identify", "provide",
        "விளக்குக", "விளக்கு", "வரையறு", "கூறுக", "பட்டியலிடுக"
    )

    // Standard interrogative question words
    private val QUESTION_WORDS = listOf(
        "what", "why", "how", "when", "where", "who", "whom", "which",
        "என்ன", "எப்படி", "ஏன்", "எங்கே", "யார்", "எவை"
    )

    // Inversion auxiliary verbs that begin inquiry questions
    private val INVERSION_WORDS = listOf(
        "is", "are", "do", "does", "did", "can", "could", "would", "should", "will"
    )

    /**
     * Strips leading greeting prefixes such as "Hi, ", "Hello, ", "Good morning! "
     */
    fun stripGreetingPrefix(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("""^(?i)(?:hi|hello|hey|good\s+morning|good\s+afternoon|good\s+evening)[,\s!.-]+""")
        return trimmed.replace(regex, "").trim()
    }

    /**
     * Checks if the input is an identity inquiry (e.g. "Who are you?", "What are you?", "Hi, what are you?").
     */
    fun isIdentityOrAssistant(input: String): Boolean {
        val lower = normalizeForClassification(input)
        if (IDENTITY_PATTERNS.any { lower == it || lower.contains(it) }) return true
        if (CAPABILITY_PATTERNS.any { lower == it || lower.contains(it) }) return true
        return false
    }

    /**
     * Checks if the input is a conversational greeting (e.g. "Hi", "Hello", "How are you?").
     */
    fun isGreeting(input: String): Boolean {
        val lower = normalizeForClassification(input)
        if (STANDALONE_GREETINGS.contains(lower)) return true

        // Compound greetings like "hi how are you" or "hello how are you doing" or "hi there"
        if (lower.startsWith("hi ") || lower.startsWith("hello ") || lower.startsWith("hey ")) {
            val rest = lower.replace(Regex("""^(hi|hello|hey)\s+"""), "").trim()
            if (rest in STANDALONE_GREETINGS || rest == "there") return true
        }

        return false
    }

    /**
     * Classifies the input text into a high-level QuestionCategory.
     */
    fun classify(input: String): QuestionCategory {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return QuestionCategory.STATEMENT

        // 1. Standalone mathematical expression (e.g. "2+2", "10*5", "sqrt(64)")
        if (MathExpressionSolver.hasMathExpression(trimmed)) {
            return QuestionCategory.MATHEMATICAL
        }

        val lower = normalizeForClassification(trimmed)

        // 2. Identity Inquiry takes precedence (e.g. "Hi, what are you?", "Who are you?")
        if (IDENTITY_PATTERNS.any { lower == it || lower.contains(it) }) {
            return QuestionCategory.GENERAL_IDENTITY
        }

        // 3. Capability Inquiry (e.g. "What can you do?", "How do you work?")
        if (CAPABILITY_PATTERNS.any { lower == it || lower.contains(it) }) {
            return QuestionCategory.GENERAL_ASSISTANT
        }

        // 4. Standalone Greetings (e.g. "Hi", "Hello", "How are you?")
        if (isGreeting(trimmed)) {
            return QuestionCategory.GREETING
        }

        // 5. Check if query without greeting prefix is a lecture question (e.g. "Hello, what is 2NF?")
        val stripped = stripGreetingPrefix(trimmed)
        val queryToCheck = if (stripped.isNotBlank()) stripped else trimmed

        if (isLectureQuestionCore(queryToCheck)) {
            return QuestionCategory.LECTURE_RELATED
        }

        return QuestionCategory.STATEMENT
    }

    /**
     * Requirement 8: Identifies the exact QuestionCategory of the student input:
     * - GREETING
     * - GENERAL_KNOWLEDGE
     * - MATHEMATICS
     * - PROGRAMMING
     * - SCIENCE
     * - LECTURE_RELATED
     * - COMPARISON
     * - DEFINITION
     * - UNSUPPORTED
     */
    fun classifyCategory(input: String, hasLectureContext: Boolean = false): QuestionCategory {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return QuestionCategory.UNSUPPORTED

        // 1. Mathematics (Arithmetic, Equations, Powers, Roots, Fractions)
        if (MathExpressionSolver.hasMathExpression(trimmed)) {
            return QuestionCategory.MATHEMATICS
        }

        val lower = normalizeForClassification(trimmed)

        // 2. Greetings
        if (isGreeting(trimmed)) {
            return QuestionCategory.GREETING
        }

        // 3. Identity / Assistant capabilities
        if (IDENTITY_PATTERNS.any { lower == it || lower.contains(it) } || CAPABILITY_PATTERNS.any { lower == it || lower.contains(it) }) {
            return QuestionCategory.GENERAL_KNOWLEDGE
        }

        // 4. Comparison ("difference between", "compare", "vs", "versus")
        if (lower.contains("difference between") || lower.contains("compare") || lower.contains(" versus ") || lower.contains(" vs ")) {
            return QuestionCategory.COMPARISON
        }

        val stripped = stripGreetingPrefix(trimmed)
        val queryToCheck = if (stripped.isNotBlank()) stripped else trimmed
        val lowerQuery = normalizeForClassification(queryToCheck)

        // 5. Explicit Definition inquiries ("define ...", "definition of ...")
        if (lowerQuery.startsWith("define ") || lowerQuery.startsWith("definition of ") ||
            lowerQuery.startsWith("meaning of ") || lowerQuery.startsWith("what is the meaning of ")
        ) {
            if (hasLectureContext && isLectureQuestionCore(queryToCheck)) return QuestionCategory.LECTURE_RELATED
            return QuestionCategory.DEFINITION
        }

        // 6. Programming & Computer Science
        val programmingKeywords = listOf(
            "code", "programming", "java", "kotlin", "python", "c++", "c language", "pointer",
            "coroutine", "oop", "polymorphism", "inheritance", "encapsulation", "abstraction",
            "data structure", "array", "linked list", "stack", "queue", "hash table", "hashmap",
            "binary search", "bubble sort", "merge sort", "quick sort", "big o",
            "acid", "database", "sql", "normalization", "1nf", "2nf", "3nf", "bcnf", "join",
            "operating system", "deadlock", "virtual memory", "paging", "process", "thread",
            "osi model", "tcp", "udp", "nullpointerexception", "segmentation fault"
        )
        if (programmingKeywords.any { lowerQuery.contains(it) }) {
            if (hasLectureContext && isLectureQuestionCore(queryToCheck)) {
                return QuestionCategory.LECTURE_RELATED
            }
            return QuestionCategory.PROGRAMMING
        }

        // 7. Science (Physics, Chemistry, Biology)
        val scienceKeywords = listOf(
            "physics", "chemistry", "biology", "newton", "gravity", "ohm", "atom",
            "chemical bond", "covalent", "ionic", "acid", "base", "ph scale",
            "photosynthesis", "cellular respiration", "dna", "rna", "mitosis", "meiosis",
            "ecosystem", "animal", "plant", "cell", "organism"
        )
        if (scienceKeywords.any { lowerQuery.contains(it) }) {
            if (hasLectureContext && isLectureQuestionCore(queryToCheck)) {
                return QuestionCategory.LECTURE_RELATED
            }
            return QuestionCategory.SCIENCE
        }

        // 8. General Definition questions ("what is ...", "what are ...")
        if (lowerQuery.startsWith("what is ") || lowerQuery.startsWith("what are ") ||
            lowerQuery.startsWith("what do you mean by ")
        ) {
            if (hasLectureContext) return QuestionCategory.LECTURE_RELATED
            return QuestionCategory.DEFINITION
        }

        // 8. Lecture-related if context is active and matches
        if (hasLectureContext && isLectureQuestionCore(queryToCheck)) {
            return QuestionCategory.LECTURE_RELATED
        }

        // 9. General educational inquiry
        if (isLectureQuestionCore(queryToCheck)) {
            return QuestionCategory.GENERAL_KNOWLEDGE
        }

        return QuestionCategory.UNSUPPORTED
    }

    /**
     * Determines whether the given text represents a question, greeting, or mathematical expression.
     * Returns false for ordinary lecture statements (e.g. "Database normalization reduces data redundancy.").
     */
    fun isQuestion(input: String): Boolean {
        val category = classify(input)
        return category != QuestionCategory.STATEMENT
    }

    /**
     * Core syntax check for lecture questions and inquiries.
     */
    private fun isLectureQuestionCore(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        // Explicit question mark
        if (trimmed.endsWith("?")) {
            return true
        }

        val lower = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("""^[0-9]+[.\-)]\s*"""), "")
            .replace(Regex("""^speaker\s*\d*\s*:\s*"""), "")
            .replace(",", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Directive commands ("explain ...", "define ...", "name ...", "calculate ...")
        for (prefix in DIRECTIVE_PREFIXES) {
            if (lower == prefix || lower.startsWith("$prefix ") || lower.startsWith("$prefix\t")) {
                if (lower.length > prefix.length + 2) {
                    return true
                }
            }
        }

        // Starts with a standard question word ("what", "why", "how", etc.)
        val words = lower.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val firstWord = words.firstOrNull() ?: ""

        if (firstWord in QUESTION_WORDS) {
            return true
        }

        // Tamil question phrases and markers (e.g. "என்றால் என்ன", "என்பது என்ன", "விளக்குக")
        if (lower.contains("என்றால் என்ன") || lower.contains("என்பது என்ன") || lower.contains("விளக்குக") || lower.contains("வரையறு")) {
            return true
        }

        // Inversion question (e.g. "Is normalization a process?", "Can machines think?")
        if (firstWord in INVERSION_WORDS && words.size >= 3) {
            return true
        }

        return false
    }

    private fun normalizeForClassification(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace("!", "")
            .replace(".", "")
            .replace(",", " ")
            .replace(Regex("""^[0-9]+[.\-)]\s*"""), "")
            .replace(Regex("""^speaker\s*\d*\s*:\s*"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Extracts individual questions, greetings, and inquiries from a multi-sentence or multiline text block.
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

