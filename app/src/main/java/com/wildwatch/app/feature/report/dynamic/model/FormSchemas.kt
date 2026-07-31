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
            id = "_2_4_What_is_the_age_the_Mountain_gorilla",
            label = "2.4 What is the age level of the Mountain gorilla?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(
                Choice("mature", "Adult"),
                Choice("young", "Juvenile"),
                Choice("i_do_not_know", "Infant"),
                Choice("not_sure", "Not sure")
            ),
            visibilityCondition = { isSelected(it, "_2_2_Which_wild_animal_have_yo", "1__mountain_gorilla") }
        ),
        Question(
            id = "_2_7_What_is_the_heal_us_of_the_Elephant_s",
            label = "2.7 What is the health status of the Elephant(s)?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(
                Choice("healthy", "Healthy"),
                Choice("weak", "Weak"),
                Choice("injured", "Injured"),
                Choice("dead", "Dead")
            ),
            visibilityCondition = { isSelected(it, "_2_2_Which_wild_animal_have_yo", "10__elephants") }
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
            id = "Have_you_seen_the_wild_animal_",
            label = "2.1. Have you seen any wild animal in the community?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(Choice("yes", "Yes"), Choice("no", "No")),
            isRequired = true
        ),
        Question(
            id = "_3_1_0_When_could_ha_ntered_the_community",
            label = "3.1.0. When could have the wild animal entered the community?",
            type = QuestionType.SELECT_ONE,
            choices = listOf(
                Choice("today", "Today"),
                Choice("yesterday", "Yesterday"),
                Choice("last_two_days", "Last two days"),
                Choice("last_three_days", "Last three days")
            ),
            visibilityCondition = { isSelected(it, "Have_you_seen_the_wild_animal_", "no") }
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
            id = "_2_1_2_Which_crops_were_raided",
            label = "3.1.3. Which crops were raided on?",
            type = QuestionType.SELECT_MULTIPLE,
            choices = listOf(
                Choice("beans", "Beans (Ebihimba)"),
                Choice("cassava", "Cassava (Muhogo)"),
                Choice("coffee", "Coffee (Omwaani)"),
                Choice("maize", "Maize (Ebikyoori)"),
                Choice("bnana__ebitookye", "Banana (Ebitookye)"),
                Choice("others", "Others")
            ),
            visibilityCondition = { isSelected(it, "What_effect_conflict_has_the_", "crops_raiding") }
        ),
        Question(
            id = "_3_3_Livestock_killed_Eaten_by_",
            label = "3.1.1. Which livestock was affected?",
            type = QuestionType.SELECT_MULTIPLE,
            choices = listOf(
                Choice("chicken", "Chicken"),
                Choice("goat", "Goat"),
                Choice("pig", "Pig"),
                Choice("others", "Others")
            ),
            visibilityCondition = { isSelected(it, "What_effect_conflict_has_the_", "livestock_raiding") }
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
