package com.wildwatch.app.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.wildwatch.app.core.ui.component.BackHeader

private val BWINDI_CENTER = LatLng(-1.03, 29.66)

@Composable
fun CommunityMapScreen(
    onBack: () -> Unit,
    onReportSighting: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BWINDI_CENTER, 12f)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
            ) {
                for (incident in uiState.incidents) {
                    Marker(
                        state = remember(incident.id) { MarkerState(position = LatLng(incident.lat, incident.lng)) },
                        title = incident.species,
                        snippet = incident.locationName ?: incident.community,
                        icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(incident.severity)),
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
            ) {
                BackHeader(
                    title = "Activity Map",
                    subtitle = "Live sightings in Bwindi",
                    onBack = onBack
                )
            }

            Button(
                onClick = onReportSighting,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("Report New Sighting", fontWeight = FontWeight.Bold)
            }
        }
    }
}
