package com.wildwatch.app.feature.publicmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.wildwatch.app.feature.tracking.ATTRACTION_MARKER_ICON_SIZE
import com.wildwatch.app.feature.tracking.MapControlButton
import com.wildwatch.app.feature.tracking.rememberAttractionMarkerIcon

// Public/tourist counterpart to RangerTrackingScreen - browse-only (park
// attractions as markers, satellite toggle, recenter on me). No incidents, no
// danger zones, no add-POI or patrol controls; see PublicMapViewModel for why
// this is a separate screen rather than a "read-only mode" flag.
@Composable
fun PublicMapScreen(viewModel: PublicMapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(Point.fromLngLat(29.66, -1.03))
        }
    }

    LaunchedEffect(uiState.activePark) {
        uiState.activePark?.let { park ->
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(park.center)
                    .zoom(park.zoomLevel)
                    .build(),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = { MapStyle(uiState.mapStyleUri) },
        ) {
            uiState.attractions.forEach { attraction ->
                val icon = rememberAttractionMarkerIcon(attraction.type)
                PointAnnotation(point = attraction.point) {
                    iconImage = icon
                    iconSize = ATTRACTION_MARKER_ICON_SIZE
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapControlButton(
                icon = if (uiState.isSatelliteView) Icons.Default.Terrain else Icons.Default.Layers,
                onClick = viewModel::toggleMapStyle,
            )
            MapControlButton(
                icon = Icons.Default.MyLocation,
                onClick = {
                    uiState.userLocation?.let { location ->
                        viewportState.flyTo(CameraOptions.Builder().center(location).zoom(14.0).build())
                    }
                },
            )
        }
    }
}
