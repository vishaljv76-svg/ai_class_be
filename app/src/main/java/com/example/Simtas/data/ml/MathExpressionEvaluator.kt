package com.example.Simtas.data.ml

import com.example.Simtas.ui.notes.DetectedQuestionAnswerItem

/**
 * Dedicated Math Expression Evaluator component.
 * Evaluates mathematical calculations and arithmetic questions offline.
 *
 * Supports expressions such as:
 * - "What is 2 + 2?" -> 4
 * - "Solve 15 × 5." -> 75
 * - "100 / 4" -> 25
 * - "sqrt(64)" -> 8
 * - "20% of 200" -> 40
 */
object MathExpressionEvaluator {

    /**
     * Checks if the query contains a mathematical calculation or expression.
     */
    fun canEvaluate(query: String): Boolean {
        return MathExpressionSolver.hasMathExpression(query)
    }

    /**
     * Evaluates a mathematical query and returns a structured Q&A item if solvable.
     */
    fun evaluate(query: String): DetectedQuestionAnswerItem? {
        val solution = MathExpressionSolver.solveTranscript(query) ?: return null
        if (!solution.isSuccess) return null

        return DetectedQuestionAnswerItem(
            question = solution.question,
            answer = solution.answer,
            explanation = solution.explanation,
            status = "Answered",
            isAnswered = true
        )
    }
}

