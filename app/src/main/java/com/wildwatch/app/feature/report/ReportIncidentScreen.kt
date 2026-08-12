package com.wildwatch.app.feature.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.component.FieldLabel
import com.wildwatch.app.core.ui.component.PermissionDialog
import com.wildwatch.app.core.ui.component.PhotoGrid
import com.wildwatch.app.core.ui.component.WildWatchTextField

// Replaces ReportSelectionScreen (the Wildlife Sighting / Human-Wildlife Conflict split) and
// the whole remote-schema-driven DynamicReportScreen with one fast, fixed-field form: a
// category picker, description, optional photos, and species (sighting only). GPS/park/
// timestamp are captured automatically in ReportIncidentViewModel and never shown as fields -
// see locationStatusText() below for the only UI surface that GPS capture gets at all.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    draftId: String?,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadCurrentLocation()
    }

    LaunchedEffect(draftId) {
        viewModel.initialize(draftId)
        // GPS is meant to be fully automatic (see the file header comment) - that only
        // actually happens if permission is requested here rather than left to fail
        // silently. initialize() already attempted a (harmless, gracefully-handled) capture
        // above; this dialog, if accepted, retries it via the launcher callback below.
        if (!hasLocationPermission) {
            showLocationPermissionDialog = true
        }
    }

    if (showLocationPermissionDialog) {
        PermissionDialog(
            icon = Icons.Filled.LocationOn,
            title = "Allow WildWatch to use your location?",
            description = "We use your location to automatically tag where this report happened - you won't need to enter it yourself.",
            onAllow = {
                showLocationPermissionDialog = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showLocationPermissionDialog = false },
        )
    }

    LaunchedEffect(uiState.savedIncidentId) {
        uiState.savedIncidentId?.let {
            viewModel.consumeSavedEvent()
            onSubmitted(it)
        }
    }

    val hasUnsavedContent = uiState.description.isNotBlank() || uiState.species.isNotBlank() || uiState.photos.isNotEmpty()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (hasUnsavedContent && !uiState.isSaving) showExitDialog = true else onBack()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Save as draft?") },
            text = { Text("You have unsaved changes. Would you like to save this report as a draft and finish it later?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(asDraft = true)
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Save Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(
                title = if (draftId != null) "Edit Report" else "Report Incident",
                onBack = { if (hasUnsavedContent) showExitDialog = true else onBack() },
                actions = {
                    TextButton(onClick = { viewModel.save(asDraft = true) }, enabled = !uiState.isSaving) {
                        Text("Save Draft", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    FieldLabel("What are you reporting?")
                    CategoryChipRow(
                        selected = uiState.type,
                        onSelect = viewModel::selectType,
                    )
                }

                AnimatedVisibility(
                    visible = uiState.type == IncidentType.SIGHTING,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        FieldLabel("Species")
                        WildWatchTextField(
                            value = uiState.species,
                            onValueChange = viewModel::updateSpecies,
                            placeholder = "e.g. Mountain Gorilla, Elephant",
                        )
                    }
                }

                Column {
                    FieldLabel("What happened?")
                    WildWatchTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        placeholder = "Briefly describe what you saw or what happened",
                        singleLine = false,
                        minLines = 3,
                    )
                }

                Column {
                    FieldLabel("Photo (optional)")
                    PhotoGrid(
                        photoUris = uiState.photos,
                        onAddPhoto = onNavigateToCamera,
                        onRemovePhoto = viewModel::removePhoto,
                        slots = 3,
                    )
                }

                LocationStatusRow(
                    isLoading = uiState.isLocationLoading,
                    locationName = uiState.locationName,
                    error = uiState.locationError,
                    onRetry = {
                        if (hasLocationPermission) {
                            viewModel.loadCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                )

                uiState.saveError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.canSubmit && !uiState.isSaving,
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Submit Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class CategoryOption(val type: IncidentType, val label: String, val icon: ImageVector)

private val categoryOptions = listOf(
    CategoryOption(IncidentType.SIGHTING, "Sighting", Icons.Filled.Visibility),
    CategoryOption(IncidentType.CONFLICT, "Conflict", Icons.Filled.Warning),
    CategoryOption(IncidentType.POACHING, "Poaching", Icons.Filled.GppBad),
    CategoryOption(IncidentType.EMERGENCY, "Emergency", Icons.Filled.Emergency),
    CategoryOption(IncidentType.SNARE, "Snare", Icons.Filled.Dangerous),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipRow(selected: IncidentType, onSelect: (IncidentType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categoryOptions, key = { it.type }) { option ->
            FilterChip(
                selected = option.type == selected,
                onClick = { onSelect(option.type) },
                label = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun LocationStatusRow(
    isLoading: Boolean,
    locationName: String?,
    error: String?,
    onRetry: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(6.dp))
        val text = when {
            isLoading -> "Detecting your location…"
            error != null -> "Location unavailable - you can still submit"
            locationName != null -> locationName
            else -> "Location not detected"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (error != null) {
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
