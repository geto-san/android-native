package com.wildwatch.app.feature.report.dynamic.model

import kotlinx.serialization.Serializable

@Serializable
data class CanonicalFormSchema(
    val schemaVersion: String,
    val forms: Map<String, CanonicalFormDefinition>,
)

@Serializable
data class CanonicalFormDefinition(
    val questions: List<CanonicalQuestionDefinition>,
)

@Serializable
data class CanonicalQuestionDefinition(
    val id: String,
    val label: String,
    val type: String,
    val choices: List<CanonicalChoiceDefinition> = emptyList(),
    val required: Boolean = false,
    val relevance: String? = null,
    val group: String? = null,
)

@Serializable
data class CanonicalChoiceDefinition(
    val id: String,
    val label: String,
)
