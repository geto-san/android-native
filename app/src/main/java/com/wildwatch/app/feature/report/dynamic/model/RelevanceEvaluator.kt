package com.wildwatch.app.feature.report.dynamic.model

object RelevanceEvaluator {
    private val selectedPattern =
        Regex("""selected\s*\(\s*([^,]+?)\s*,\s*'((?:\\'|[^'])*)'\s*\)""")

    fun evaluate(expression: String?, answers: Map<String, Any?>): Boolean {
        if (expression.isNullOrBlank()) return true
        return parseOr(expression.trim(), answers)
    }

    fun toVisibilityCondition(expression: String?): ((Map<String, Any?>) -> Boolean)? {
        if (expression.isNullOrBlank()) return null
        return { answers -> evaluate(expression, answers) }
    }

    private fun parseOr(expression: String, answers: Map<String, Any?>): Boolean =
        splitTopLevel(expression, " or ").any { parseAnd(it, answers) }

    private fun parseAnd(expression: String, answers: Map<String, Any?>): Boolean =
        splitTopLevel(expression, " and ").all { parseAtom(it, answers) }

    private fun parseAtom(atom: String, answers: Map<String, Any?>): Boolean {
        val trimmed = atom.trim()
        if (trimmed.isEmpty()) return true

        selectedPattern.matchEntire(trimmed)?.let { match ->
            val questionId = match.groupValues[1].trim().removePrefix("/")
            val value = match.groupValues[2].replace("\\'", "'")
            return isSelected(answers, questionId, value)
        }

        val notEqualIndex = trimmed.indexOf("!=")
        if (notEqualIndex >= 0) {
            val questionId = trimmed.substring(0, notEqualIndex).trim().removePrefix("/")
            val expected = parseLiteral(trimmed.substring(notEqualIndex + 2).trim())
            return !valueEquals(answers[questionId], expected)
        }

        val equalIndex = trimmed.indexOf('=')
        if (equalIndex >= 0) {
            val questionId = trimmed.substring(0, equalIndex).trim().removePrefix("/")
            val expected = parseLiteral(trimmed.substring(equalIndex + 1).trim())
            return valueEquals(answers[questionId], expected)
        }

        return isTruthy(answers[trimmed.removePrefix("/")])
    }


    private fun splitTopLevel(expression: String, delimiter: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        var index = 0

        while (index <= expression.length - delimiter.length) {
            when (expression[index]) {
                '(' -> depth++
                ')' -> depth = maxOf(0, depth - 1)
            }

            if (depth == 0 && expression.regionMatches(index, delimiter, 0, delimiter.length)) {
                parts.add(expression.substring(start, index))
                index += delimiter.length
                start = index
                continue
            }
            index++
        }

        parts.add(expression.substring(start))
        return parts
    }

    private fun parseLiteral(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith('\'') && trimmed.endsWith('\'')) {
            return trimmed.substring(1, trimmed.length - 1).replace("\\'", "'")
        }
        return trimmed
    }

    private fun valueEquals(answer: Any?, expected: String): Boolean = when (answer) {
        null -> expected.isEmpty()
        is String -> answer == expected
        is Number -> answer.toString() == expected
        is List<*> -> answer.any { it?.toString() == expected }
        else -> answer.toString() == expected
    }

    private fun isTruthy(answer: Any?): Boolean = when (answer) {
        null -> false
        is String -> answer.isNotBlank()
        is Collection<*> -> answer.isNotEmpty()
        else -> true
    }
}
