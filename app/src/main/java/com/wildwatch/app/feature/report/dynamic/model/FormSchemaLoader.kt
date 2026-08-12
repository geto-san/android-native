package com.wildwatch.app.feature.report.dynamic.model

import android.content.Context
import com.wildwatch.app.core.database.IncidentType
import kotlinx.serialization.json.Json
import timber.log.Timber

object FormSchemaLoader {
    private const val ASSET_PATH = "forms/wildwatch_schema.json"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun loadQuestions(context: Context, type: IncidentType): List<Question>? {
        val formKey = when (type) {
            IncidentType.SIGHTING -> "sighting"
            IncidentType.CONFLICT -> "conflict"
            else -> return emptyList()
        }

        return try {
            val schema = loadSchema(context)
            schema.forms[formKey]?.questions?.map { it.toQuestion() }
        } catch (error: Exception) {
            Timber.w(error, "Failed to load bundled form schema for %s", formKey)
            null
        }
    }

    private fun loadSchema(context: Context): CanonicalFormSchema {
        val payload = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return json.decodeFromString<CanonicalFormSchema>(payload)
    }

    private fun CanonicalQuestionDefinition.toQuestion(): Question {
        return Question(
            id = id,
            label = label,
            type = when {
                id.equals("Date", ignoreCase = true) -> QuestionType.DATE
                id.equals("Time", ignoreCase = true) -> QuestionType.TIME
                id.contains("GPS", ignoreCase = true) -> QuestionType.GPS
                label.startsWith("Hello", ignoreCase = true) || label.startsWith("Welcome", ignoreCase = true) -> QuestionType.HEADER
                type == "select_one" -> QuestionType.SELECT_ONE
                type == "select_multiple" -> QuestionType.SELECT_MULTIPLE
                type == "photos" -> QuestionType.PHOTOS
                type == "header" -> QuestionType.HEADER
                else -> QuestionType.TEXT
            },
            choices = choices.map { Choice(it.id, it.label) },
            isRequired = required,
            visibilityCondition = RelevanceEvaluator.toVisibilityCondition(relevance),
        )
    }
}
