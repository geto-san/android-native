package com.wildwatch.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.theme.Cream

// The map didn't appear in any wireframe, but wireframe 5's Home shows a
// "Live community map" card - this is that map, full-screen, restyled to the
// app's new visual language (BackHeader, rounded surfaces, forest-green
// accents) instead of the old ranger app's plain Material chrome.
@Composable
fun CommunityMapScreen(
    onBack: () -> Unit,
    onReportSighting: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by rememberSaveable {
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
        if (hasLocationPermission) viewModel.loadCurrentLocation() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BWINDI_CENTER, 12f)
    }
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 14f)
        }
    }

    var mapType by rememberSaveable { mutableStateOf(MapType.NORMAL) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            BackHeader(title = "Community Map", subtitle = "Live sightings & incidents near you", onBack = onBack)

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission, mapType = mapType),
                    ) {
                        for (incident in uiState.incidents) {
                            Marker(
                                state = remember(incident.id) {
                                    MarkerState(position = LatLng(incident.lat, incident.lng))
                                },
                                title = incident.species,
                                snippet = incident.locationName ?: "${incident.lat}, ${incident.lng}",
                                icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(incident.severity)),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    Icon(Icons.Filled.Layers, contentDescription = "Toggle satellite view")
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            )
                            Text(
                                "Live",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        Text(
                            "${uiState.incidents.size} report${if (uiState.incidents.size == 1) "" else "s"} on the map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                        )
                        PillButton(
                            text = "Report a sighting",
                            leadingIcon = Icons.Filled.CameraAlt,
                            onClick = onReportSighting,
                            contentColor = Cream,
                        )
                    }
                }
            }
        }
    }
}

private val BWINDI_CENTER = LatLng(-1.03, 29.66)
