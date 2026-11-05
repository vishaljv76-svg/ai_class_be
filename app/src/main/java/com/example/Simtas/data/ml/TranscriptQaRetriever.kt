package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem
import java.util.Locale

/**
 * Dedicated Transcript Question-Answering Retriever component.
 * Executes Step 1 of the question answering search hierarchy:
 * Searches the current lecture transcript for ground truth sentences that answer the query.
 *
 * Strict safety rules:
 * - Rejects sentences that are questions or identical to the query.
 * - Prioritizes definitions and explanatory clauses.
 * - Returns null when the concept is absent, allowing downstream fallthrough.
 */
object TranscriptQaRetriever {

    private val STOP_WORDS = setOf(
        "a", "an", "the", "and", "or", "but", "if", "in", "on", "at", "to", "for", "with",
        "of", "by", "from", "up", "about", "into", "through", "after", "is", "are", "was",
        "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "this",
        "that", "these", "those", "what", "which", "who", "whom", "whose", "why", "how",
        "some", "any", "all", "each", "every", "both", "few", "more", "most", "other"
    )

    /**
     * Searches the transcript for a direct answer to the question.
     */
    fun retrieveAnswer(question: String, transcriptText: String): DetectedQuestionAnswerItem? {
        val cleanTranscript = transcriptText.trim()
        if (cleanTranscript.isBlank()) return null

        val cleanQ = question.trim()
        val cleanLower = cleanQ.lowercase(Locale.ROOT)
            .replace("?", "")
            .replace(",", " ")
            .replace(
                Regex("""^(what is|what are|why is|why are|how does|how do|explain|define|describe|clarify|tell me about|name\s+some|name|list\s+some|list|give\s+some|give|mention\s+some|mention|identify|state)\s+"""),
                ""
            )
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanLower.isBlank()) return null

        val queryKeywords = cleanLower.split("\\s+".toRegex())
            .filter { it.length > 2 && it !in STOP_WORDS }

        if (queryKeywords.isEmpty()) return null

        // Split transcript into candidate sentences
        val sentences = cleanTranscript.split(Regex("""(?<=[.!?])\s+|\n+"""))
            .map { it.trim() }
            .filter { it.length >= 12 }

        var bestSentence: String? = null
        var bestScore = 0

        for (sentence in sentences) {
            val sentenceLower = sentence.lowercase(Locale.ROOT)

            // Reject sentences that are themselves questions or identical to the query
            if (QuestionDetector.isQuestion(sentence)) continue
            if (sentence.equals(cleanQ, ignoreCase = true)) continue
            val sentenceAlnum = sentenceLower.replace(Regex("""[^a-z0-9]"""), "")
            val queryAlnum = cleanQ.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
            if (sentenceAlnum == queryAlnum) continue

            // Direct definition match: e.g. "X is defined as Y" or "X is the process of Y"
            if (sentenceLower.contains(cleanLower) && (
                    sentenceLower.contains(" is ") || sentenceLower.contains(" are ") ||
                    sentenceLower.contains(" means ") || sentenceLower.contains(" refers to ") ||
                    sentenceLower.contains(" defined as ") || sentenceLower.contains(" requires ") ||
                    sentenceLower.contains(" removes ")
                )) {
                return DetectedQuestionAnswerItem(
                    question = cleanQ,
                    answer = sentence,
                    explanation = "Extracted directly from the lecture transcript.",
                    status = "Answered from Transcript",
                    isAnswered = true
                )
            }

            // Keyword overlap score
            var score = 0
            var keywordMatches = 0
            for (kw in queryKeywords) {
                if (sentenceLower.contains(kw)) {
                    score += 4
                    keywordMatches++
                }
            }

            // A candidate sentence MUST contain at least one query keyword
            if (keywordMatches == 0) continue

            // Direct full query match bonus (e.g. sentence contains "2nf")
            if (sentenceLower.contains(cleanLower)) {
                score += 5
            }

            // Bonus for explanatory indicators
            if (sentenceLower.contains("is defined as") || sentenceLower.contains("refers to") || sentenceLower.contains("means that")) {
                score += 3
            }
            if (sentenceLower.contains("requires") || sentenceLower.contains("eliminates") || sentenceLower.contains("removes") || sentenceLower.contains("ensures") || sentenceLower.contains("prevents")) {
                score += 2
            }

            val minRequiredKeywords = if (queryKeywords.size <= 2) queryKeywords.size else 2
            if (score > bestScore && keywordMatches >= minRequiredKeywords) {
                bestScore = score
                bestSentence = sentence
            }
        }

        if (bestSentence != null) {
            return DetectedQuestionAnswerItem(
                question = cleanQ,
                answer = bestSentence,
                explanation = "Extracted directly from the lecture transcript.",
                status = "Answered from Transcript",
                isAnswered = true
            )
        }

        return null
    }
}
