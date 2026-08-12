package com.wildwatch.app.feature.report.dynamic.model

enum class QuestionType {
    TEXT,
    NUMBER,
    SELECT_ONE,
    SELECT_MULTIPLE,
    SEVERITY,
    PHOTOS,
    DATE,
    TIME,
    GPS,
    HEADER
}

data class Choice(
    val id: String,
    val label: String
)

data class Question(
    val id: String,
    val label: String,
    val type: QuestionType,
    val choices: List<Choice> = emptyList(),
    val placeholder: String = "",
    val isRequired: Boolean = false,
    val visibilityCondition: ((Map<String, Any?>) -> Boolean)? = null
)

enum class FormViewMode {
    FLOW, // Vertical scroll
    PAGING // One by one
}
