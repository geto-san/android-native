package com.wildwatch.app.feature.report.dynamic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.ui.component.*
import com.wildwatch.app.feature.report.dynamic.model.Choice
import com.wildwatch.app.feature.report.dynamic.model.Question
import com.wildwatch.app.feature.report.dynamic.model.QuestionType

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuestionItem(
    question: Question,
    answer: Any?,
    onAnswerChanged: (Any?) -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(question.label)
        Spacer(modifier = Modifier.height(12.dp))
        
        when (question.type) {
            QuestionType.TEXT -> {
                WildWatchTextField(
                    value = answer?.toString() ?: "",
                    onValueChange = onAnswerChanged,
                    placeholder = question.placeholder,
                    singleLine = false,
                    minLines = 3
                )
            }
            QuestionType.NUMBER -> {
                WildWatchTextField(
                    value = answer?.toString() ?: "",
                    onValueChange = { onAnswerChanged(it.filter { c -> c.isDigit() }) },
                    placeholder = question.placeholder,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            }
            QuestionType.SELECT_ONE -> {
                val selectedChoice = question.choices.find { it.id == answer?.toString() }
                WildWatchDropdownField(
                    value = selectedChoice,
                    options = question.choices,
                    onSelect = { onAnswerChanged(it?.id) },
                    displayName = { it?.label ?: "" }
                )
            }
            QuestionType.SELECT_MULTIPLE -> {
                val selectedIds = (answer as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    question.choices.forEach { choice ->
                        val isSelected = selectedIds.contains(choice.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) selectedIds - choice.id else selectedIds + choice.id
                                onAnswerChanged(next)
                            },
                            label = { Text(choice.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
            QuestionType.SEVERITY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentSeverity = answer as? IncidentSeverity
                    SeverityChip(
                        label = "Low",
                        value = IncidentSeverity.LOW,
                        selected = currentSeverity == IncidentSeverity.LOW,
                        onClick = { onAnswerChanged(IncidentSeverity.LOW) },
                        modifier = Modifier.weight(1f)
                    )
                    SeverityChip(
                        label = "Medium",
                        value = IncidentSeverity.MEDIUM,
                        selected = currentSeverity == IncidentSeverity.MEDIUM,
                        onClick = { onAnswerChanged(IncidentSeverity.MEDIUM) },
                        modifier = Modifier.weight(1f)
                    )
                    SeverityChip(
                        label = "High",
                        value = IncidentSeverity.HIGH,
                        selected = currentSeverity == IncidentSeverity.HIGH,
                        onClick = { onAnswerChanged(IncidentSeverity.HIGH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            QuestionType.PHOTOS -> {
                val photoUris = (answer as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                PhotoGrid(
                    photoUris = photoUris,
                    onAddPhoto = onAddPhoto,
                    onRemovePhoto = { uri -> onAnswerChanged(photoUris - uri) },
                    slots = 3
                )
            }
        }
    }
}
