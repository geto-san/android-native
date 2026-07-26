package com.wildwatch.app.feature.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangerMapScreen(
    viewModel: RangerMapViewModel = hiltViewModel()
) {
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
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState
        ) {
            // Note: Mapbox v11 handles style via standard plugins or MapInitOptions
            
            if (uiState.showAttractions) {
                uiState.attractions.forEach { attraction ->
                    PointAnnotation(
                        point = attraction.location
                    ) {
                        // In v11, icon properties are often set inside this init block
                        // using something like iconImage("icon-name")
                    }
                }
            }
        }

        // Collapsible Search Bar (M3 SearchBar)
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onSearch = { viewModel.updateSearchQuery(it) },
            active = uiState.isSearching,
            onActiveChange = { if (!it) viewModel.clearSearch() },
            placeholder = { Text("Search park locations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { 
                if (uiState.isSearching) {
                    IconButton(onClick = viewModel::clearSearch) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .statusBarsPadding()
        ) {
            // Suggestion list logic
        }

        // Vertical Sidebar
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapControlButton(
                icon = if (uiState.isSatelliteView) Icons.Default.Terrain else Icons.Default.Layers,
                onClick = viewModel::toggleMapStyle
            )
            MapControlButton(
                icon = if (uiState.is3DMode) Icons.Default.ViewInAr else Icons.Default.ViewHeadline,
                onClick = viewModel::toggle3DMode
            )
            MapControlButton(
                icon = if (uiState.showAttractions) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onClick = viewModel::toggleAttractionsVisibility
            )
            MapControlButton(
                icon = Icons.Default.MyLocation,
                onClick = {
                    uiState.userLocation?.let { location ->
                        viewportState.flyTo(CameraOptions.Builder().center(location).zoom(14.0).build())
                    }
                }
            )
        }
    }
}

@Composable
private fun MapControlButton(
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
