package com.example.Simtas.data.ml

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

data class MathSolution(
    val isSuccess: Boolean,
    val question: String,
    val rawInput: String,
    val answer: String,
    val numericValue: Double? = null,
    val explanation: String = ""
)

/**
 * Separate, offline Mathematical Expression Solver.
 * Recognizes and safely calculates mathematical expressions in transcripts such as:
 * - 2+2
 * - 10*5
 * - 100/4
 * - 50-20
 * - 20% of 200
 * - sqrt(64)
 *
 * Safe recursive-descent parsing. ZERO arbitrary code execution.
 */
object MathExpressionSolver {

    private val numberWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
        "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
        "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
        "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
    )

    // Regex to match standalone math expressions like 2+2, 10*5, 100/4, 50-20, 20% of 200, sqrt(64)
    private val STANDALONE_MATH_REGEX = Regex(
        """(?i)\b([0-9.]+)\s*(\+|\-|\*|/|×|÷|x|\^)\s*([0-9.]+)\b|""" +
                """(?i)\b([0-9.]+)\s*(?:%|percent)\s*of\s*([0-9.]+)\b|""" +
                """(?i)(?:the\s+)?(?:square\s+root|sqrt|cube\s+root|cbrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)|""" +
                """(?i)\b([0-9.]+)\s*(?:squared|cubed)\b|""" +
                """(?i)\b([0-9.]+)\s*(?:to\s+the\s+power\s+(?:of\s+)?|\^)\s*([0-9.]+)\b|""" +
                """(?i)\b([0-9.]+)\s*(\+|\-|\*|/|×|÷)\s*([0-9.]+)\s*(\+|\-|\*|/|×|÷)\s*([0-9.]+)\b|""" +
                """(?i)\(?\s*([0-9.]+)\s*(\+|\-|\*|/|×|÷)\s*([0-9.]+)\s*\)?\s*(\*|/|\+|\-)\s*([0-9.]+)"""
    )

    /**
     * Checks if the transcript contains any mathematical expression.
     * Works with raw expressions (e.g. "2+2") or natural language ("What is 2+2?").
     */
    fun hasMathExpression(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        // Check for equation solving pattern: e.g. "solve 2x + 6 = 14" or "2x + 6 = 14"
        if (trimmed.contains("=") && (trimmed.contains("x", ignoreCase = true) || trimmed.contains("solve", ignoreCase = true))) {
            val hasDigit = trimmed.any { it.isDigit() }
            if (hasDigit) return true
        }

        // Check standalone pattern
        if (STANDALONE_MATH_REGEX.containsMatchIn(trimmed)) {
            return true
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        val wordKeywords = listOf(
            "sqrt", "cbrt", "plus", "minus", "times", "multiplied", "divided", "percent of", "% of",
            "square root", "cube root", "squared", "cubed", "power of", "solve for x", "solve equation"
        )
        val hasWordOp = wordKeywords.any { lower.contains(it) }
        val hasSymbolOp = Regex("""(?<![a-zA-Z])[\+\-\*\/×÷\^%](?![a-zA-Z])""").containsMatchIn(lower)
        val hasDigit = lower.any { it.isDigit() } || numberWords.keys.any { lower.contains(it) }

        return (hasWordOp || hasSymbolOp) && hasDigit
    }

    /**
     * Solves a transcript containing a mathematical expression or equation.
     * Returns a structured MathSolution if found, or null if ordinary lecture text.
     */
    fun solveTranscript(transcriptText: String): MathSolution? {
        val trimmed = transcriptText.trim()
        if (trimmed.isEmpty()) return null

        // Strip speaker/timestamp headers like "Speaker 1: 2+2" or "[00:01] Speaker 1: 2+2"
        val clean = trimmed.replace(Regex("""(?i)(?:\[[\d:]+\]\s*)?(?:Speaker\s*\d+|User|Lecturer|Teacher)\s*:\s*"""), "").trim()
        val textToProcess = if (clean.isNotBlank()) clean else trimmed

        if (!hasMathExpression(textToProcess)) {
            return null
        }

        // Check for linear equation first (e.g. "solve for x: 2x + 6 = 14")
        val equationSolution = solveLinearEquation(textToProcess)
        if (equationSolution != null) {
            return equationSolution
        }

        // 1. Try to find standalone expression match
        val isPureMathExpression = textToProcess.all { it.isDigit() || it.isWhitespace() || it in "+-*/×÷^%.()" }
        val targetExpression = if (isPureMathExpression) {
            textToProcess
        } else {
            val match = STANDALONE_MATH_REGEX.find(textToProcess)
            if (match != null) match.value.trim() else textToProcess
        }

        return solve(targetExpression, textToProcess)
    }

    /**
     * Solves linear equations of the form ax + b = c or ax = c with step-by-step steps.
     */
    fun solveLinearEquation(text: String): MathSolution? {
        val lower = text.lowercase(Locale.ROOT)
            .replace("solve for x:", "")
            .replace("solve for x", "")
            .replace("solve equation", "")
            .replace("solve", "")
            .replace("?", "")
            .trim()

        if (!lower.contains("=") || !lower.contains("x")) return null

        val sides = lower.split("=")
        if (sides.size != 2) return null

        val leftStr = sides[0].trim()
        val rightStr = sides[1].trim()

        val rightVal = rightStr.toDoubleOrNull() ?: return null

        // Pattern: ax + b  or  ax - b  or  ax
        val linearRegex = Regex("""^([+-]?\s*\d*(?:\.\d+)?)\s*x\s*([+-]\s*\d+(?:\.\d+)?)?$""")
        val match = linearRegex.find(leftStr) ?: return null

        val aRaw = match.groupValues[1].replace(" ", "")
        val a = when {
            aRaw.isEmpty() || aRaw == "+" -> 1.0
            aRaw == "-" -> -1.0
            else -> aRaw.toDoubleOrNull() ?: return null
        }
        if (a == 0.0) return null

        val bRaw = match.groupValues[2].replace(" ", "")
        val b = if (bRaw.isNotEmpty()) bRaw.toDoubleOrNull() ?: 0.0 else 0.0

        // ax + b = c => ax = c - b => x = (c - b) / a
        val cMinusB = rightVal - b
        val xVal = cMinusB / a
        val formattedX = formatResult(xVal)
        val formattedA = formatResult(a)
        val formattedB = formatResult(abs(b))
        val formattedC = formatResult(rightVal)

        val steps = StringBuilder()
        steps.append("Equation: $leftStr = $rightStr\n")
        if (b != 0.0) {
            val opWord = if (b > 0) "Subtract $formattedB from" else "Add $formattedB to"
            steps.append("Step 1: $opWord both sides: ${if (a == 1.0) "x" else "${formattedA}x"} = ${formatResult(cMinusB)}\n")
            if (a != 1.0) {
                steps.append("Step 2: Divide both sides by $formattedA: x = $formattedX\n")
            }
        } else if (a != 1.0) {
            steps.append("Step 1: Divide both sides by $formattedA: x = $formattedX\n")
        }
        steps.append("Result: x = $formattedX")

        return MathSolution(
            isSuccess = true,
            question = text.trim(),
            rawInput = text.trim(),
            answer = "x = $formattedX",
            numericValue = xVal,
            explanation = steps.toString()
        )
    }

    /**
     * Solves a specific expression and generates formatted question, answer, and explanation.
     */
    fun solve(expressionText: String, originalInput: String = expressionText): MathSolution? {
        val normalized = normalizeExpression(expressionText)
        if (normalized.isBlank()) return null

        return try {
            val parser = Parser(normalized)
            val value = parser.parse()
            val formattedAnswer = formatResult(value)

            val displayQuestion = formatDisplayQuestion(expressionText, normalized)
            val explanation = buildExplanation(expressionText, normalized, formattedAnswer)

            MathSolution(
                isSuccess = true,
                question = displayQuestion,
                rawInput = originalInput.trim(),
                answer = formattedAnswer,
                numericValue = value,
                explanation = explanation
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeExpression(query: String): String {
        var text = query.trim().lowercase(Locale.ROOT)

        val prefixes = listOf(
            "can you calculate", "can you solve", "can you tell me what is",
            "what is the result of", "what is the value of", "what is", "calculate",
            "evaluate", "solve", "how much is", "find", "compute", "tell me"
        )
        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                text = text.removePrefix(prefix).trim()
            }
        }
        text = text.replace("?", "").replace("=", "").trim()

        // Replace number words with digits
        val words = text.split("\\s+".toRegex()).toMutableList()
        for (i in words.indices) {
            numberWords[words[i]]?.let { digit ->
                words[i] = digit
            }
        }
        text = words.joinToString(" ")

        // Handle square root: sqrt(64) or sqrt 64 or square root of 64
        val sqrtRegex = Regex("""(?:the\s+)?(?:square\s+root|sqrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)""")
        text = sqrtRegex.replace(text) { m ->
            "sqrt(${m.groupValues[1]})"
        }

        // Handle cube root: cbrt(27) or cube root of 27
        val cbrtRegex = Regex("""(?:the\s+)?(?:cube\s+root|cbrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)""")
        text = cbrtRegex.replace(text) { m ->
            "cbrt(${m.groupValues[1]})"
        }

        // Handle natural language powers
        text = Regex("""([0-9.]+)\s*(?:to\s+the\s+power\s+(?:of\s+)?|\^)\s*([0-9.]+)""").replace(text, "($1 ^ $2)")
        text = Regex("""([0-9.]+)\s*squared""").replace(text, "($1 ^ 2)")
        text = Regex("""([0-9.]+)\s*cubed""").replace(text, "($1 ^ 3)")

        // Handle fraction to percentage: e.g. "3/4 as a percentage"
        text = Regex("""([0-9.]+)\s*/\s*([0-9.]+)\s*(?:as\s+a\s+percentage|to\s+percentage)""").replace(text) { m ->
            "((${m.groupValues[1]} / ${m.groupValues[2]}) * 100)"
        }

        // Handle percentage: 20% of 200 or 20 percent of 200
        val percentOfRegex = Regex("""([0-9.]+)\s*(?:%|percent)\s*of\s*([0-9.]+)""")
        text = percentOfRegex.replace(text) { m ->
            "((${m.groupValues[1]} / 100) * ${m.groupValues[2]})"
        }

        // Replace operator words and symbols
        text = text.replace("multiplied by", "*")
            .replace("times", "*")
            .replace("into", "*")
            .replace("divided by", "/")
            .replace("over", "/")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("×", "*")
            .replace("÷", "/")

        // Replace letter x when used as multiplication between numbers e.g. 10x5
        text = Regex("""([0-9]+)\s*x\s*([0-9]+)""").replace(text, "$1 * $2")

        return text.replace("\\s+".toRegex(), " ").trim()
    }

    private fun formatDisplayQuestion(raw: String, normalized: String): String {
        val lower = raw.trim().lowercase(Locale.ROOT)
            .removePrefix("what is").removePrefix("calculate").removePrefix("solve").trim()
            .replace("?", "").trim()

        // Percentage
        val pctMatch = Regex("""([0-9.]+)\s*(?:%|percent)\s*of\s*([0-9.]+)""").find(lower)
        if (pctMatch != null) {
            return "${pctMatch.groupValues[1]}% of ${pctMatch.groupValues[2]}"
        }

        // Square root
        val sqrtMatch = Regex("""(?:square\s+root|sqrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)""").find(lower)
        if (sqrtMatch != null) {
            return if (lower.contains("sqrt")) "sqrt(${sqrtMatch.groupValues[1]})" else "√${sqrtMatch.groupValues[1]}"
        }

        // Basic arithmetic: format cleanly with spaces around operators
        val basic = lower.replace("+", " + ")
            .replace("-", " - ")
            .replace("*", " * ")
            .replace("×", " × ")
            .replace("/", " / ")
            .replace("÷", " ÷ ")
            .replace("multiplied by", "×")
            .replace("times", "×")
            .replace("divided by", "÷")
            .replace("plus", "+")
            .replace("minus", "-")

        return basic.replace("\\s+".toRegex(), " ").trim()
    }

    private fun buildExplanation(raw: String, normalized: String, answer: String): String {
        val lower = raw.trim().lowercase(Locale.ROOT)

        // Percentage: 20% of 200 -> "20 percent of 200 gives 40."
        val pctMatch = Regex("""([0-9.]+)\s*(?:%|percent)\s*of\s*([0-9.]+)""").find(lower)
        if (pctMatch != null) {
            val a = pctMatch.groupValues[1]
            val b = pctMatch.groupValues[2]
            return "$a percent of $b gives $answer."
        }

        // Cube root
        val cbrtMatch = Regex("""(?:cube\s+root|cbrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)""").find(lower)
        if (cbrtMatch != null) {
            val a = cbrtMatch.groupValues[1]
            return "Cube root of $a gives $answer."
        }

        // Square root: sqrt(64) -> "Square root of 64 gives 8."
        val sqrtMatch = Regex("""(?:square\s+root|sqrt)\s*(?:of\s+)?(?:\(?\s*([0-9.]+)\s*\)?)""").find(lower)
        if (sqrtMatch != null) {
            val a = sqrtMatch.groupValues[1]
            return "Square root of $a gives $answer."
        }

        // Powers: 2^8 or 2 to the power of 5
        val powMatch = Regex("""([0-9.]+)\s*(?:\^|to the power of|power)\s*([0-9.]+)""").find(lower)
            ?: Regex("""([0-9.]+)\s*\^\s*([0-9.]+)""").find(normalized)
        if (powMatch != null) {
            val a = powMatch.groupValues[1]
            val b = powMatch.groupValues[2]
            return "Raising $a to the power of $b gives $answer."
        }

        // Addition: 2+2 -> "Adding 2 and 2 gives 4."
        val addMatch = Regex("""([0-9.]+)\s*(?:\+|plus)\s*([0-9.]+)""").find(lower)
            ?: Regex("""([0-9.]+)\s*\+\s*([0-9.]+)""").find(normalized)
        if (addMatch != null) {
            val a = addMatch.groupValues[1]
            val b = addMatch.groupValues[2]
            return "Adding $a and $b gives $answer."
        }

        // Multiplication: 10*5 -> "Multiplying 10 by 5 gives 50."
        val mulMatch = Regex("""([0-9.]+)\s*(?:\*|×|x|multiplied by|times)\s*([0-9.]+)""").find(lower)
            ?: Regex("""([0-9.]+)\s*\*\s*([0-9.]+)""").find(normalized)
        if (mulMatch != null) {
            val a = mulMatch.groupValues[1]
            val b = mulMatch.groupValues[2]
            return "Multiplying $a by $b gives $answer."
        }

        // Division: 100/4 -> "Dividing 100 by 4 gives 25."
        val divMatch = Regex("""([0-9.]+)\s*(?:/|÷|divided by|over)\s*([0-9.]+)""").find(lower)
            ?: Regex("""([0-9.]+)\s*/\s*([0-9.]+)""").find(normalized)
        if (divMatch != null) {
            val a = divMatch.groupValues[1]
            val b = divMatch.groupValues[2]
            return "Dividing $a by $b gives $answer."
        }

        // Subtraction: 50-20 -> "Subtracting 20 from 50 gives 30."
        val subMatch = Regex("""([0-9.]+)\s*(?:\-|minus)\s*([0-9.]+)""").find(lower)
            ?: Regex("""([0-9.]+)\s*\-\s*([0-9.]+)""").find(normalized)
        if (subMatch != null) {
            val a = subMatch.groupValues[1]
            val b = subMatch.groupValues[2]
            return "Subtracting $b from $a gives $answer."
        }

        val cleanExpr = normalized.replace("*", "×").replace("/", "÷")
        return "$cleanExpr gives $answer."
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        if (value.isNaN()) return "Undefined"

        val symbols = DecimalFormatSymbols(Locale.US)
        return if (value == floor(value) && !value.isInfinite() && abs(value) < 1e15) {
            String.format(Locale.US, "%.0f", value)
        } else {
            val df = DecimalFormat("#.######", symbols)
            df.format(value)
        }
    }

    // --- Safe Recursive Descent Parser ---
    private class Parser(private val input: String) {
        private var pos = 0
        private val length = input.length

        fun parse(): Double {
            val result = parseExpression()
            skipWhitespace()
            if (pos < length) {
                throw IllegalArgumentException("Unexpected character at position $pos: '${input[pos]}'")
            }
            return result
        }

        private fun skipWhitespace() {
            while (pos < length && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                if (pos >= length) break
                val op = input[pos]
                if (op != '+' && op != '-') break
                pos++
                val nextTerm = parseTerm()
                value = if (op == '+') value + nextTerm else value - nextTerm
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                if (pos >= length) break
                val op = input[pos]
                if (op != '*' && op != '/' && op != '%') break
                pos++
                val nextFactor = parseFactor()
                value = when (op) {
                    '*' -> value * nextFactor
                    '/' -> {
                        if (nextFactor == 0.0) throw ArithmeticException("Division by zero")
                        value / nextFactor
                    }
                    '%' -> value % nextFactor
                    else -> value
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            var value = parseUnary()
            skipWhitespace()
            if (pos < length && input[pos] == '^') {
                pos++
                val exponent = parseFactor()
                value = value.pow(exponent)
            }
            return value
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            if (pos < length) {
                if (input[pos] == '+') {
                    pos++
                    return parseUnary()
                }
                if (input[pos] == '-') {
                    pos++
                    return -parseUnary()
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (pos >= length) {
                throw IllegalArgumentException("Unexpected end of expression")
            }

            val ch = input[pos]

            if (ch == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (pos >= length || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis ')'")
                }
                pos++
                return value
            }

            if (ch.isLetter()) {
                val start = pos
                while (pos < length && input[pos].isLetter()) {
                    pos++
                }
                val funcName = input.substring(start, pos).lowercase(Locale.ROOT)
                skipWhitespace()
                if (pos >= length || input[pos] != '(') {
                    throw IllegalArgumentException("Expected '(' after function '$funcName'")
                }
                pos++
                val arg = parseExpression()
                skipWhitespace()
                if (pos >= length || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis ')' for function '$funcName'")
                }
                pos++

                return when (funcName) {
                    "sqrt" -> {
                        if (arg < 0) throw ArithmeticException("Cannot calculate square root of a negative number")
                        sqrt(arg)
                    }
                    "cbrt" -> Math.cbrt(arg)
                    "abs" -> abs(arg)
                    "round" -> round(arg)
                    "floor" -> floor(arg)
                    "ceil" -> ceil(arg)
                    else -> throw IllegalArgumentException("Unknown mathematical function: '$funcName'")
                }
            }

            if (ch.isDigit() || ch == '.') {
                val start = pos
                var hasDot = false
                while (pos < length) {
                    val c = input[pos]
                    if (c.isDigit()) {
                        pos++
                    } else if (c == '.' && !hasDot) {
                        hasDot = true
                        pos++
                    } else {
                        break
                    }
                }
                val numStr = input.substring(start, pos)
                return numStr.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid number format: '$numStr'")
            }

            throw IllegalArgumentException("Unexpected token at position $pos: '$ch'")
        }
    }
}

