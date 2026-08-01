package com.wildwatch.app.feature.report.dynamic.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Drives dynamic form state. Visibility is resolved from each [Question.visibilityCondition],
 * which [FormSchemaLoader] builds from bundled relevance expressions via [RelevanceEvaluator].
 */
class FormEngine(
    val questions: List<Question>,
) {
    private val _answers = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val answers: StateFlow<Map<String, Any?>> = _answers.asStateFlow()

    private val _visibleQuestions = MutableStateFlow<List<Question>>(emptyList())
    val visibleQuestions: StateFlow<List<Question>> = _visibleQuestions.asStateFlow()

    init {
        updateVisibleQuestions()
    }

    fun updateAnswer(questionId: String, answer: Any?) {
        _answers.update { it + (questionId to answer) }
        updateVisibleQuestions()
    }

    private fun updateVisibleQuestions() {
        val currentAnswers = _answers.value
        val visible = questions.filter { question ->
            question.visibilityCondition?.invoke(currentAnswers) ?: true
        }
        _visibleQuestions.value = visible
    }
    
    fun getAnswer(questionId: String): Any? = _answers.value[questionId]
}
