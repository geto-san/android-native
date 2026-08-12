package com.wildwatch.app.feature.report.dynamic.model

import com.wildwatch.app.core.database.IncidentSeverity

fun isSelected(answers: Map<String, Any?>, qId: String, value: String): Boolean {
    val ans = answers[qId]
    return when (ans) {
        is String -> ans == value
        is List<*> -> ans.contains(value)
        else -> false
    }
}

object FormSchemas {
    val SightingForm = listOf(
        Question(
            id = "District",
            label = "1.2 District",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("kisoro", "Kisoro"), Choice("kiruhura", "Isingiro")),
            isRequired = true
        ),
        Question(
            id = "Sub_county",
            label = "1.3 Sub-county",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("default", "Default Sub-county")),
            isRequired = true
        ),
        Question(
            id = "GPS_Coordinates",
            label = "GPS Coordinates",
            type = QuestionType.TEXT,
            isRequired = true
        ),
        Question(
            id = "Have_you_seen_the_wild_animal_",
            label = "2.1. Have you seen any wild animal in the community?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("yes", "Yes"), Choice("no", "No")),
            isRequired = true
        ),
        Question(
            id = "_2_2_Which_wild_animal_have_yo",
            label = "2.2. Which wild animal have you seen?",
            type = QuestionType.SELECT_MULTIPLE,
            choices = listOf(
                Choice("1__mountain_gorilla", "Mountain gorilla"),
                Choice("2__baboons", "Baboons"),
                Choice("3__black_and_white_colobus", "Black and white colobus"),
                Choice("4__blue_monkey", "Blue monkey"),
                Choice("6__bush_pigs", "Bush pigs"),
                Choice("8__chimpanzee", "Chimpanzee"),
                Choice("10__elephants", "Elephants"),
                Choice("20__others", "Others")
            ),
            visibilityCondition = { isSelected(it, "Have_you_seen_the_wild_animal_", "yes") },
            isRequired = true
        ),
        Question(
            id = "photos",
            label = "2.3 Take a picture of the wild animal(s)",
            type = QuestionType.PHOTOS,
            visibilityCondition = { isSelected(it, "Have_you_seen_the_wild_animal_", "yes") }
        ),
        Question(
            id = "description",
            label = "Observation Notes",
            type = QuestionType.TEXT,
            placeholder = "Describe the situation...",
            isRequired = true
        )
    )

    val ConflictForm = listOf(
        Question(
            id = "District",
            label = "1.2 District",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("kisoro", "Kisoro"), Choice("kiruhura", "Isingiro")),
            isRequired = true
        ),
        Question(
            id = "Sub_county",
            label = "1.3 Sub-county",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("default", "Default Sub-county")),
            isRequired = true
        ),
        Question(
            id = "GPS_Coordinates",
            label = "GPS Coordinates",
            type = QuestionType.TEXT,
            isRequired = true
        ),
        Question(
            id = "Have_you_seen_the_wild_animal_",
            label = "2.1. Have you seen any wild animal in the community?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("yes", "Yes"), Choice("no", "No")),
            isRequired = true
        ),
        Question(
            id = "What_effect_conflict_has_the_",
            label = "3.1. Nature of human-wild conflict?",
            type = QuestionType.SELECT_MULTIPLE,
            choices = listOf(
                Choice("crops_raiding", "Crop raiding"),
                Choice("livestock_raiding", "Live stock raiding"),
                Choice("structure_destruction", "Structure destruction"),
                Choice("other_conflict", "Human injury/other")
            ),
            isRequired = true
        ),
        Question(
            id = "severity",
            label = "Severity level",
            type = QuestionType.SEVERITY,
            isRequired = true
        ),
        Question(
            id = "description",
            label = "Incident Details",
            type = QuestionType.TEXT,
            placeholder = "Describe the damage and situation...",
            isRequired = true
        ),
        Question(
            id = "photos",
            label = "Evidence Photos",
            type = QuestionType.PHOTOS
        )
    )
}

