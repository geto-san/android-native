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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.WildWatchDropdownField
import com.wildwatch.app.ui.components.WildWatchTextField
import com.wildwatch.app.ui.theme.Destructive
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SPECIES_OPTIONS = listOf("African Elephant", "Cape Buffalo", "Hippopotamus", "Baboon", "Hyena", "Other")
private val DAMAGE_OPTIONS = listOf("Crop damage", "Livestock killed", "Property damage", "Human injury", "Human death", "Other")
private val CROP_OPTIONS = listOf("Maize", "Bananas", "Cassava", "Coffee", "Beans", "Not applicable")

private enum class PhotoCategory(val key: String, val label: String) {
    ANIMAL("animal", "Animal"),
    SCENE("scene", "Scene / situation"),
    DAMAGE("damage", "Damage or injury"),
}

// wireframe 11 & 11b: the urgent Human-Wildlife Conflict report.
@Composable
fun ConflictReportScreen(
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    onNavigateToCamera: (String) -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var speciesInvolved by remember { mutableStateOf(SPECIES_OPTIONS.first()) }
    var typeOfDamage by remember { mutableStateOf(DAMAGE_OPTIONS.first()) }
    var cropAffected by remember { mutableStateOf(CROP_OPTIONS.first()) }
    var areaAffected by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val now = remember { java.time.LocalDateTime.now() }

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
        viewModel.updateType(IncidentType.CONFLICT)
        viewModel.updateSeverity(Severity.HIGH)
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

    val galleryPickerCategory = remember { mutableStateOf<PhotoCategory?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val category = galleryPickerCategory.value
        if (uri != null && category != null) viewModel.addPhoto(uri.toString(), category.key)
    }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.wildwatch.app.ui.components.BackHeader(title = "Wildlife Incident", subtitle = "Reported as urgent", onBack = onBack)

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Destructive.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Destructive, modifier = Modifier.size(20.dp))
                    Text(
                        buildString {
                            append("All wildlife incidents are treated as urgent and dispatched to the nearest ranger immediately.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "AUTO-CAPTURED",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AutoCapturedField(
                                icon = Icons.Filled.CalendarToday,
                                label = "DATE",
                                value = now.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                modifier = Modifier.weight(1f),
                            )
                            AutoCapturedField(
                                icon = Icons.Filled.Schedule,
                                label = "TIME",
                                value = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AutoCapturedField(
                                icon = Icons.Filled.Park,
                                label = "PARK",
                                value = parkDisplayName(uiState.park),
                                modifier = Modifier.weight(1f),
                            )
                            AutoCapturedField(
                                icon = Icons.Filled.LocationOn,
                                label = "GPS",
                                value = if (uiState.latitude != null) {
                                    "%.4f, %.4f".format(uiState.latitude, uiState.longitude)
                                } else {
                                    "Locating…"
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            "GPS is locked at submission — the incident location will not change if you move afterwards.",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FieldLabel("SPECIES INVOLVED")
                        WildWatchDropdownField(value = speciesInvolved, options = SPECIES_OPTIONS, onSelect = { speciesInvolved = it }, displayName = { it })

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("TYPE OF DAMAGE")
                        WildWatchDropdownField(value = typeOfDamage, options = DAMAGE_OPTIONS, onSelect = { typeOfDamage = it }, displayName = { it })

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("CROP AFFECTED")
                        WildWatchDropdownField(value = cropAffected, options = CROP_OPTIONS, onSelect = { cropAffected = it }, displayName = { it })

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("APPROX. AREA AFFECTED")
                        WildWatchTextField(value = areaAffected, onValueChange = { areaAffected = it }, placeholder = "e.g. 0.5 acres")

                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("DESCRIPTION (OPTIONAL)")
                        WildWatchTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = "Add any extra detail...",
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
                        Text(
                            "Photos of the animal, the scene, and any damage or injury. Rangers and UWA officials will see these.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PhotoCategory.entries.forEach { category ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(category.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            CategorizedPhotoRow(
                                photos = uiState.categorizedPhotoUris[category.key].orEmpty(),
                                onCamera = { onNavigateToCamera(category.key) },
                                onUpload = {
                                    galleryPickerCategory.value = category
                                    galleryLauncher.launch("image/*")
                                },
                                onRemove = { uri -> viewModel.removePhoto(uri, category.key) },
                            )
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
                        text = "Submit urgent report",
                        loading = uiState.isSaving,
                        containerColor = Destructive,
                        onClick = {
                            viewModel.updateSpecies(speciesInvolved)
                            viewModel.updateCategory(typeOfDamage)
                            val details = buildString {
                                append("Crop affected: $cropAffected.")
                                if (areaAffected.isNotBlank()) append(" Area affected: $areaAffected.")
                                if (description.isNotBlank()) append(" $description")
                            }
                            viewModel.updateSummary(details)
                            viewModel.save()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun AutoCapturedField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun CategorizedPhotoRow(
    photos: List<String>,
    onCamera: () -> Unit,
    onUpload: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        photos.take(3).forEach { uri ->
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
                IconButton(onClick = { onRemove(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(22.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White)
                }
            }
        }
        if (photos.size < 3) {
            PhotoAddTile(icon = Icons.Filled.AddAPhoto, onClick = onCamera, modifier = Modifier.weight(1f))
        }
        if (photos.size < 2) {
            PhotoAddTile(icon = Icons.Filled.UploadFile, onClick = onUpload, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PhotoAddTile(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun parkDisplayName(park: Park): String = park.name
    .split("_")
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
