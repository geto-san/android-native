package com.wildwatch.app.feature.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Severity
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.component.PermissionDialog
import com.wildwatch.app.core.ui.component.WildWatchDropdownField
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500
import androidx.compose.material.icons.filled.LocationOn
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    var isRecording by remember { mutableStateOf(false) }
    val observedAt = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }

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
                        photoUris = uiState.photoUris,
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

@Composable
internal fun PhotoGrid(
    photoUris: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    slots: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        photoUris.take(slots).forEach { uri ->
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small).background(Grey200),
                )
                IconButton(
                    onClick = { onRemovePhoto(uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        repeat((slots - photoUris.size).coerceAtLeast(0)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(Grey200)
                    .clickable(onClick = onAddPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo", tint = Grey500)
            }
        }
    }
}
