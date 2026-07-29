package com.wildwatch.app.feature.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.ui.component.*
import com.wildwatch.app.core.ui.theme.Destructive
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

    LaunchedEffect(Unit) {
        viewModel.updateType(IncidentType.CONFLICT)
        viewModel.loadCurrentLocation()
        
        if (viewModel.behavior.isEmpty()) viewModel.behavior = CONFLICT_TYPES.first()
        if (viewModel.species.isEmpty()) viewModel.species = SPECIES_OPTIONS.first()
    }
    LaunchedEffect(uiState.savedIncidentId) {
        uiState.savedIncidentId?.let {
            viewModel.consumeSavedEvent()
            onSubmitted(it)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(), 
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "Conflict Report", subtitle = "Human-wildlife impact", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Surface(
                        color = SunsetAmber.copy(alpha = 0.1f), // Increased alpha for visibility
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
                    WildWatchDropdownField(
                        value = viewModel.behavior, 
                        options = CONFLICT_TYPES, 
                        onSelect = { viewModel.behavior = it }, 
                        displayName = { it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Species Involved")
                    WildWatchDropdownField(
                        value = viewModel.species, 
                        options = SPECIES_OPTIONS, 
                        onSelect = { viewModel.species = it }, 
                        displayName = { it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Severity")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SeverityChip(
                            label = "Low", 
                            value = IncidentSeverity.LOW, 
                            selected = uiState.severity == IncidentSeverity.LOW, 
                            onClick = { viewModel.updateSeverity(IncidentSeverity.LOW) }, 
                            modifier = Modifier.weight(1f)
                        )
                        SeverityChip(
                            label = "Medium", 
                            value = IncidentSeverity.MEDIUM, 
                            selected = uiState.severity == IncidentSeverity.MEDIUM, 
                            onClick = { viewModel.updateSeverity(IncidentSeverity.MEDIUM) }, 
                            modifier = Modifier.weight(1f)
                        )
                        SeverityChip(
                            label = "High", 
                            value = IncidentSeverity.HIGH, 
                            selected = uiState.severity == IncidentSeverity.HIGH, 
                            onClick = { viewModel.updateSeverity(IncidentSeverity.HIGH) }, 
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Description")
                    WildWatchTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.description = it },
                        placeholder = "Describe the incident and damage…",
                        singleLine = false,
                        minLines = 4,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    FieldLabel("Evidence Photos")
                    PhotoGrid(
                        photoUris = uiState.localImageUris,
                        onAddPhoto = { onNavigateToCamera(viewModel.behavior) },
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
                        onClick = { viewModel.save() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Destructive),
                        enabled = uiState.canSubmit && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Submitting..." else "Submit Report", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
