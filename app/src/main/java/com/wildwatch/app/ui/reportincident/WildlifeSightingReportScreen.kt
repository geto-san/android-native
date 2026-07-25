package com.wildwatch.app.ui.reportincident

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.StatusPill
import com.wildwatch.app.ui.components.WildWatchDropdownField
import com.wildwatch.app.ui.components.WildWatchTextField
import com.wildwatch.app.ui.theme.Info
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SPECIES_OPTIONS = listOf(
    "African Elephant", "Mountain Gorilla", "Cape Buffalo", "Grey Crowned Crane",
    "Chimpanzee", "Lion", "Leopard", "Other",
)
private val BEHAVIOR_OPTIONS = listOf("Foraging", "Resting", "Traveling", "Alert / Fleeing", "Nesting", "Other")

// wireframe 9 & 9b.
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadCurrentLocation()
    }
    LaunchedEffect(Unit) {
        viewModel.updateType(IncidentType.SIGHTING)
        viewModel.updateSeverity(Severity.LOW)
        if (hasLocationPermission) viewModel.loadCurrentLocation() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            BackHeader(title = "Wildlife Sighting", subtitle = "Report what you saw", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FieldLabel("SPECIES")
                        WildWatchDropdownField(value = species, options = SPECIES_OPTIONS, onSelect = { species = it }, displayName = { it })

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("NUMBER OBSERVED")
                        WildWatchTextField(
                            value = numberObserved,
                            onValueChange = { numberObserved = it.filter(Char::isDigit) },
                            placeholder = "e.g. 5",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("BEHAVIOR")
                        WildWatchDropdownField(value = behavior, options = BEHAVIOR_OPTIONS, onSelect = { behavior = it }, displayName = { it })

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("DATE & TIME")
                        WildWatchTextField(value = "Today · $observedAt", onValueChange = {}, readOnly = true)

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("DESCRIPTION")
                        WildWatchTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = "What did you observe?",
                            singleLine = false,
                            minLines = 3,
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FieldLabel("PHOTO EVIDENCE")
                        Spacer(modifier = Modifier.height(8.dp))
                        PhotoGrid(
                            photoUris = uiState.photoUris,
                            onAddPhoto = onNavigateToCamera,
                            onRemovePhoto = { viewModel.removePhoto(it) },
                            slots = 3,
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            FieldLabel("VOICE NOTE", modifier = Modifier.weight(1f))
                            StatusPill(text = "Optional", contentColor = Info)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                                .padding(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { isRecording = !isRecording },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                if (isRecording) "Recording…" else "Tap to record up to 60s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                uiState.saveError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Box(modifier = Modifier.padding(vertical = 20.dp)) {
                    PillButton(
                        text = "Submit sighting",
                        loading = uiState.isSaving,
                        enabled = species.isNotBlank(),
                        onClick = {
                            viewModel.updateSpecies(species)
                            viewModel.updateCategory(behavior)
                            val countText = numberObserved.ifBlank { null }?.let { "Observed $it. " } ?: ""
                            viewModel.updateSummary("$countText$description".trim())
                            viewModel.save()
                        },
                    )
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
internal fun PhotoGrid(
    photoUris: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    slots: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        photoUris.take(slots).forEach { uri ->
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                )
                IconButton(
                    onClick = { onRemovePhoto(uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White)
                }
            }
        }
        repeat((slots - photoUris.size).coerceAtLeast(0)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .clickable(onClick = onAddPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
