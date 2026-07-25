package com.wildwatch.app.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Severity
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.component.WildWatchDropdownField
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.SunsetAmber

private val CONFLICT_TYPES = listOf("Crop Damage", "Livestock Predation", "Property Damage", "Human Injury", "Other")
private val SPECIES_OPTIONS = listOf("Elephant", "Lion", "Leopard", "Buffalo", "Hyena", "Other")

@Composable
fun ConflictReportScreen(
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    onNavigateToCamera: (String) -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var conflictType by remember { mutableStateOf(CONFLICT_TYPES.first()) }
    var species by remember { mutableStateOf(SPECIES_OPTIONS.first()) }
    var severity by remember { mutableStateOf(Severity.MEDIUM) }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.updateType(IncidentType.CONFLICT)
        viewModel.loadCurrentLocation()
    }
    LaunchedEffect(uiState.savedIncidentId) {
        uiState.savedIncidentId?.let {
            viewModel.consumeSavedEvent()
            onSubmitted(it)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "Conflict Report", subtitle = "Human-wildlife impact", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Surface(
                        color = SunsetAmber.copy(alpha = 0.05f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = SunsetAmber, modifier = Modifier.size(16.dp))
                            Text(
                                "Verified reports are eligible for compensation.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Impact Type")
                    WildWatchDropdownField(value = conflictType, options = CONFLICT_TYPES, onSelect = { conflictType = it }, displayName = { it })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Species Involved")
                    WildWatchDropdownField(value = species, options = SPECIES_OPTIONS, onSelect = { species = it }, displayName = { it })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Severity")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SeverityChip("Low", Severity.LOW, severity == Severity.LOW, onClick = { severity = Severity.LOW }, modifier = Modifier.weight(1f))
                        SeverityChip("Medium", Severity.MEDIUM, severity == Severity.MEDIUM, onClick = { severity = Severity.MEDIUM }, modifier = Modifier.weight(1f))
                        SeverityChip("High", Severity.HIGH, severity == Severity.HIGH, onClick = { severity = Severity.HIGH }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Description")
                    WildWatchTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Describe the incident and damage…",
                        singleLine = false,
                        minLines = 4,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    FieldLabel("Evidence Photos")
                    PhotoGrid(
                        photoUris = uiState.photoUris,
                        onAddPhoto = { onNavigateToCamera(conflictType) },
                        onRemovePhoto = { viewModel.removePhoto(it) },
                        slots = 3,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    uiState.saveError?.let { error ->
                        Text(
                            text = error, 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.bodySmall, 
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateCategory(conflictType)
                            viewModel.updateSpecies(species)
                            viewModel.updateSeverity(severity)
                            viewModel.updateSummary(description)
                            viewModel.save()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = ButtonDefaults.buttonColors(containerColor = Destructive),
                        enabled = !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Submitting..." else "Submit Report", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(label: String, value: Severity, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = when (value) {
        Severity.LOW -> MaterialTheme.colorScheme.primary
        Severity.MEDIUM -> SunsetAmber
        Severity.HIGH -> Destructive
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) color.copy(alpha = 0.1f) else Grey200,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
        modifier = modifier.height(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) color else Grey500)
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Grey500,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
