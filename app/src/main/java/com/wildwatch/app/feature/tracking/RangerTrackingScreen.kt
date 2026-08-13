package com.wildwatch.app.feature.tracking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.MapboxMapScope
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.wildwatch.app.R
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.tracking.PatrolTrackingService
import com.wildwatch.app.core.ui.component.PermissionDialog
import com.wildwatch.app.core.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangerTrackingScreen(
    onIncidentClick: (String) -> Unit,
    viewModel: RangerTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(Point.fromLngLat(29.66, -1.03))
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showPatrolPermissionDialog by remember { mutableStateOf(false) }

    val patrolPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            context.startForegroundService(
                PatrolTrackingService.startIntent(context, uiState.activePark?.id),
            )
        }
    }

    fun startPatrolTracking() {
        if (hasLocationPermission) {
            context.startForegroundService(
                PatrolTrackingService.startIntent(context, uiState.activePark?.id),
            )
        } else {
            showPatrolPermissionDialog = true
        }
    }

    if (showPatrolPermissionDialog) {
        PermissionDialog(
            icon = Icons.Default.LocationOn,
            title = "Allow WildWatch to track your patrol route?",
            description = "While a patrol is active, we record your route in the background so " +
                "your park has an accurate record of ground covered, even if you switch apps.",
            onAllow = {
                showPatrolPermissionDialog = false
                patrolPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showPatrolPermissionDialog = false },
        )
    }

    LaunchedEffect(uiState.activePark) {
        uiState.activePark?.let { park ->
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(park.center)
                    .zoom(park.zoomLevel)
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            onMapClickListener = OnMapClickListener { point ->
                if (uiState.isPlacingPoi) {
                    viewModel.onMapTappedWhilePlacingPoi(point)
                    true
                } else {
                    false
                }
            },
            style = { com.mapbox.maps.extension.compose.style.MapStyle(uiState.mapStyleUri) },
        ) {
            // Mapbox's own scale bar/logo/attribution ornaments, disabled outright rather
            // than repositioned - a ranger operating this screen in the field doesn't need
            // them, and they compete for space with the map controls.
            MapEffect(Unit) { mapView ->
                mapView.scalebar.enabled = false
                mapView.logo.enabled = false
                mapView.attribution.enabled = false
            }
            TrackingMapAnnotations(uiState = uiState, onIncidentClick = onIncidentClick)
        }

        if (uiState.isPlacingPoi) {
            PlacingPoiHint(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
            )
        }

        MapControlsColumn(
            uiState = uiState,
            onToggleMapStyle = viewModel::toggleMapStyle,
            onToggle3DMode = viewModel::toggle3DMode,
            onToggleAttractionsVisibility = viewModel::toggleAttractionsVisibility,
            onToggleIncidentsVisibility = viewModel::toggleIncidentsVisibility,
            onRecenterOnUser = {
                uiState.userLocation?.let { location ->
                    viewportState.flyTo(CameraOptions.Builder().center(location).zoom(14.0).build())
                }
            },
            onToggleAddPoiMode = viewModel::toggleAddPoiMode,
            onTogglePatrol = {
                if (uiState.activePatrol != null) {
                    context.startService(PatrolTrackingService.stopIntent(context))
                } else {
                    startPatrolTracking()
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
        )

        uiState.pendingPoiPoint?.let {
            AddPoiSheet(
                isSubmitting = uiState.isSubmittingPoi,
                onDismiss = viewModel::cancelAddPoi,
                onSubmit = { name, type, description, animalSpecies ->
                    viewModel.submitPoi(name, type, description, animalSpecies)
                },
            )
        }
    }
}

@Composable
@MapboxMapComposable
private fun MapboxMapScope.TrackingMapAnnotations(
    uiState: RangerTrackingUiState,
    onIncidentClick: (String) -> Unit,
) {
    if (uiState.showAttractions) {
        uiState.attractions.forEach { attraction ->
            val icon = rememberAttractionMarkerIcon(attraction.type)
            PointAnnotation(point = attraction.point) {
                iconImage = icon
                iconSize = ATTRACTION_MARKER_ICON_SIZE
            }
        }
    }
    if (uiState.showIncidents) {
        uiState.incidents.forEach { incident ->
            val icon = rememberIncidentMarkerIcon(incident.type)
            PointAnnotation(point = Point.fromLngLat(incident.lng, incident.lat)) {
                iconImage = icon
                iconSize = incidentMarkerIconSize(incident.severity)
                interactionsState.onClicked {
                    onIncidentClick(incident.id)
                    true
                }
            }
        }
    }
    uiState.pendingPoiPoint?.let { pendingPoint ->
        val pendingIcon: IconImage = rememberIconImage(R.drawable.ic_marker_pending)
        PointAnnotation(point = pendingPoint) {
            iconImage = pendingIcon
            iconSize = 0.6
        }
    }
    if (uiState.patrolRoutePoints.size >= 2) {
        PolylineAnnotation(points = uiState.patrolRoutePoints) {
            lineColor = Color(0xFF2563EB)
            lineWidth = 4.0
        }
    }
}

@Composable
private fun PlacingPoiHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        color = Color.Black.copy(alpha = 0.75f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = "Tap the map to drop a pin",
            color = White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MapControlsColumn(
    uiState: RangerTrackingUiState,
    onToggleMapStyle: () -> Unit,
    onToggle3DMode: () -> Unit,
    onToggleAttractionsVisibility: () -> Unit,
    onToggleIncidentsVisibility: () -> Unit,
    onRecenterOnUser: () -> Unit,
    onToggleAddPoiMode: () -> Unit,
    onTogglePatrol: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MapControlButton(
            icon = if (uiState.isSatelliteView) Icons.Default.Terrain else Icons.Default.Layers,
            onClick = onToggleMapStyle
        )
        MapControlButton(
            icon = if (uiState.is3DMode) Icons.Default.ViewInAr else Icons.Default.ViewHeadline,
            onClick = onToggle3DMode
        )
        MapControlButton(
            icon = if (uiState.showAttractions) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            onClick = onToggleAttractionsVisibility
        )
        // toggleIncidentsVisibility() already existed on the ViewModel and worked - it just
        // had no button anywhere to reach it, so incidents could never actually be hidden.
        MapControlButton(
            icon = if (uiState.showIncidents) Icons.Default.ReportProblem else Icons.Default.ReportOff,
            onClick = onToggleIncidentsVisibility
        )
        MapControlButton(icon = Icons.Default.MyLocation, onClick = onRecenterOnUser)
        if (uiState.activePark != null) {
            MapControlButton(
                icon = if (uiState.isPlacingPoi) Icons.Default.Close else Icons.Default.AddLocationAlt,
                onClick = onToggleAddPoiMode
            )
            MapControlButton(
                icon = if (uiState.activePatrol != null) {
                    Icons.Default.StopCircle
                } else {
                    Icons.AutoMirrored.Filled.DirectionsWalk
                },
                onClick = onTogglePatrol
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPoiSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, type: AttractionType, description: String, animalSpecies: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AttractionType.DANGER_ZONE) }
    var description by remember { mutableStateOf("") }
    var animalSpecies by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add point of interest", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = type.name.replace('_', ' '),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false },
                ) {
                    AttractionType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.replace('_', ' ')) },
                            onClick = {
                                type = option
                                typeMenuExpanded = false
                            },
                        )
                    }
                }
            }

            if (type == AttractionType.ANIMAL_HABITAT) {
                OutlinedTextField(
                    value = animalSpecies,
                    onValueChange = { animalSpecies = it },
                    label = { Text("Species") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onSubmit(name, type, description, animalSpecies.ifBlank { null }) },
                enabled = name.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }
        }
    }
}

// internal (not private) so PublicMapScreen can reuse it too.
@Composable
internal fun MapControlButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = White.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )
        }
    }
}
