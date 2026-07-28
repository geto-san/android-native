package com.wildwatch.app.feature.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Severity
import com.wildwatch.app.core.ui.component.*
import com.wildwatch.app.core.ui.theme.Grey500

private val SPECIES_OPTIONS = listOf(
    "African Elephant", "Mountain Gorilla", "Cape Buffalo", "Grey Crowned Crane",
    "Chimpanzee", "Lion", "Leopard", "Other",
)
private val BEHAVIOR_OPTIONS = listOf("Foraging", "Resting", "Traveling", "Alert / Fleeing", "Nesting", "Other")

@Composable
fun WildlifeSightingReportScreen(
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var species by remember { mutableStateOf(SPECIES_OPTIONS.first()) }
    var numberObserved by remember { mutableStateOf("") }
    var behavior by remember { mutableStateOf(BEHAVIOR_OPTIONS.first()) }
    var description by remember { mutableStateOf("") }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadCurrentLocation()
    }
    LaunchedEffect(Unit) {
        viewModel.updateType(IncidentType.SIGHTING)
        viewModel.updateSeverity(Severity.LOW)
        if (hasLocationPermission) {
            viewModel.loadCurrentLocation()
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(
            icon = Icons.Default.LocationOn,
            title = "Allow WildWatch to use your location?",
            description = "We use your location to precisely tag where wildlife sightings occur, helping rangers protect endangered species.",
            onAllow = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    LaunchedEffect(uiState.locationName) {
        uiState.locationName?.let { viewModel.updateCommunity(it) }
    }
    LaunchedEffect(uiState.savedIncidentId) {
        uiState.savedIncidentId?.let {
            viewModel.consumeSavedEvent()
            onSubmitted(it)
        }
    }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "New Sighting", subtitle = "Share your observation", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    FieldLabel("Species")
                    WildWatchDropdownField(value = species, options = SPECIES_OPTIONS, onSelect = { species = it }, displayName = { it })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Number Observed")
                    WildWatchTextField(
                        value = numberObserved,
                        onValueChange = { numberObserved = it.filter(Char::isDigit) },
                        placeholder = "e.g. 5",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Behavior")
                    WildWatchDropdownField(value = behavior, options = BEHAVIOR_OPTIONS, onSelect = { behavior = it }, displayName = { it })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Observation Notes")
                    WildWatchTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Describe the situation…",
                        singleLine = false,
                        minLines = 4,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    FieldLabel("Photos")
                    PhotoGrid(
                        photoUris = uiState.mediaUris,
                        onAddPhoto = onNavigateToCamera,
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
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateSpecies(species)
                            viewModel.updateCategory(behavior)
                            val countText = numberObserved.ifBlank { null }?.let { "Observed $it. " } ?: ""
                            viewModel.updateSummary("$countText$description".trim())
                            viewModel.save()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        enabled = species.isNotBlank() && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Submitting..." else "Share Sighting", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
