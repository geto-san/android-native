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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.model.District
import com.wildwatch.app.core.model.SubCounty
import com.wildwatch.app.core.ui.component.*
import java.util.Locale

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
        viewModel.updateSeverity(IncidentSeverity.LOW)
        if (hasLocationPermission) {
            viewModel.loadCurrentLocation()
        } else {
            showPermissionDialog = true
        }
        
        // Initialize dropdown defaults if empty
        if (viewModel.species.isEmpty()) viewModel.species = SPECIES_OPTIONS.first()
        if (viewModel.behavior.isEmpty()) viewModel.behavior = BEHAVIOR_OPTIONS.first()
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
                    FieldLabel("National Park")
                    WildWatchDropdownField(
                        value = uiState.park,
                        options = Park.entries,
                        onSelect = { viewModel.updatePark(it) },
                        displayName = { it.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("District")
                    WildWatchDropdownField(
                        value = uiState.districts.find { it.id == uiState.district } ?: District(label = "Select District"),
                        options = uiState.districts,
                        onSelect = { viewModel.updateDistrict(it.id) },
                        displayName = { it.label }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (uiState.district != null) {
                    item {
                        val subCounties = uiState.districts.find { it.id == uiState.district }?.sub_counties ?: emptyList()
                        FieldLabel("Sub-county")
                        WildWatchDropdownField(
                            value = subCounties.find { it.id == uiState.subCounty } ?: SubCounty(label = "Select Sub-county"),
                            options = subCounties,
                            onSelect = { viewModel.updateSubCounty(it.id) },
                            displayName = { it.label }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (uiState.subCounty != null) {
                    item {
                        val parishes = uiState.districts.find { it.id == uiState.district }
                            ?.sub_counties?.find { it.id == uiState.subCounty }?.parishes ?: emptyList()
                        FieldLabel("Parish")
                        WildWatchDropdownField(
                            value = uiState.parish ?: "Select Parish",
                            options = parishes,
                            onSelect = { viewModel.updateParish(it) },
                            displayName = { it }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                item {
                    FieldLabel("Species")
                    WildWatchDropdownField(
                        value = viewModel.species,
                        options = SPECIES_OPTIONS,
                        onSelect = { viewModel.species = it },
                        displayName = { it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Number Observed")
                    WildWatchTextField(
                        value = viewModel.numberObserved,
                        onValueChange = { viewModel.numberObserved = it.filter(Char::isDigit) },
                        placeholder = "e.g. 5",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Behavior")
                    WildWatchDropdownField(
                        value = viewModel.behavior,
                        options = BEHAVIOR_OPTIONS,
                        onSelect = { viewModel.behavior = it },
                        displayName = { it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    FieldLabel("Observation Notes")
                    WildWatchTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.description = it },
                        placeholder = "Describe the situation…",
                        singleLine = false,
                        minLines = 4,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldLabel("Photos", modifier = Modifier.weight(1f))
                        if (uiState.isLocationLoading) {
                            Text(
                                "Fetching location...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.padding(start = 8.dp).size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    PhotoGrid(
                        photoUris = uiState.localImageUris,
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
                        onClick = { viewModel.save() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        enabled = uiState.canSubmit && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Submitting..." else "Share Sighting", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
