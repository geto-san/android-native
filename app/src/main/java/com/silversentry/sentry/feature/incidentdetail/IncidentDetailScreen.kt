package com.silversentry.sentry.feature.incidentdetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.silversentry.sentry.R
import com.silversentry.sentry.core.data.map.isMapboxTokenConfigured
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.ui.component.IconBadge
import com.silversentry.sentry.core.ui.component.StatusPill
import com.silversentry.sentry.core.ui.component.displayTitle
import com.silversentry.sentry.core.ui.component.severityColor
import com.silversentry.sentry.core.ui.component.statusColor
import com.silversentry.sentry.core.ui.component.statusLabel
import com.silversentry.sentry.core.ui.component.typeIcon
import com.silversentry.sentry.core.ui.theme.Destructive
import com.silversentry.sentry.core.ui.theme.Grey500

// The detail screen now reads like Google Maps' "ready to navigate" trip preview once you
// ask for directions: a live mini-map with the driving route from the ranger's current
// location to the incident drawn on it, a "Your location -> Destination" strip with the
// ETA/distance, and a single pinned-bottom "START RESPONSE" button (the "Start" of Google
// Maps) that opens the full-screen navigation view. Tapping it claims the incident (if not
// already the ranger's) and starts the route guidance - see
// IncidentDetailViewModel.respondToIncident() and the Route.Navigation screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    onBack: () -> Unit,
    onStartResponse: () -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadTrip() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val incident = uiState.incident
            if (incident != null && uiState.canRespond) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = {
                            viewModel.respondToIncident()
                            onStartResponse()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isAssignedToMe) "RESUME RESPONSE" else "START RESPONSE",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val incident = uiState.incident
        if (incident == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { IncidentHeader(incident) }

                if (incident.evidencePhotoUrls.isNotEmpty()) {
                    item { EvidenceGallery(incident.evidencePhotoUrls) }
                }

                if (uiState.isRanger) {
                    item {
                        TripPreviewCard(
                            origin = uiState.trip.origin,
                            destination = uiState.trip.destination,
                            routePoints = uiState.trip.route?.points.orEmpty(),
                            durationSeconds = uiState.trip.route?.durationSeconds,
                            distanceKm = uiState.trip.route?.distanceMeters?.let { it / 1000.0 }
                                ?: uiState.distanceKm,
                            isLoading = uiState.trip.isLoading,
                            destinationName = incident.locationName ?: incident.community,
                        )
                    }
                }

                item { SummaryCard(incident) }

                item { DetailsCard(incident, uiState.distanceKm) }
            }
        }
    }
}

@Composable
private fun IncidentHeader(incident: Incident) {
    val accent = severityColor(incident.severity)
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(
            icon = typeIcon(incident.type),
            background = accent.copy(alpha = 0.12f),
            tint = accent,
            size = 56.dp,
            shape = MaterialTheme.shapes.medium,
        )
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = incident.displayTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (incident.isEscalated) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = "Escalated",
                        tint = Destructive,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = statusLabel(incident.status), contentColor = statusColor(incident.status))
                StatusPill(
                    text = incident.severity.name.lowercase().replaceFirstChar { it.uppercase() },
                    contentColor = accent,
                )
            }
            if (incident.assignedToName != null) {
                Text(
                    text = "Assigned to ${incident.assignedToName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey500,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EvidenceGallery(photoUrls: List<String>) {
    Column {
        Text(
            text = "Evidence (${photoUrls.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            items(photoUrls) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

// The Google-Maps "directions preview": a live mini-map between the ranger's current
// location and the incident with the driving route drawn in, plus a "Your location ->
// Destination" row showing ETA and distance. Falls back to the straight-line haversine
// distance (no route/token/offline) rather than disappearing.
@Composable
private fun TripPreviewCard(
    origin: Point?,
    destination: Point?,
    routePoints: List<Point>,
    durationSeconds: Double?,
    distanceKm: Double?,
    isLoading: Boolean,
    destinationName: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (isMapboxTokenConfigured() && origin != null && destination != null) {
                TripMiniMap(origin, destination, routePoints)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isLoading) "Loading route…" else "Map unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Destructive,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = destinationName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.height(36.dp).width(1.dp))

                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = if (isLoading) "…" else formatEta(durationSeconds, distanceKm),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isLoading) "" else formatDistanceKm(distanceKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TripMiniMap(
    origin: Point,
    destination: Point,
    routePoints: List<Point>,
) {
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(TripCamera.centerFor(origin, destination))
            zoom(TripCamera.zoomFor(origin, destination))
        }
    }

    LaunchedEffect(routePoints, origin, destination) {
        val points = routePoints.ifEmpty { listOf(origin, destination) }
        viewportState.flyTo(
            TripCamera.optionsFor(points),
        )
    }

    val originIcon: IconImage = rememberIconImage(R.drawable.ic_marker_pending)
    val destinationIcon: IconImage = rememberIconImage(R.drawable.ic_marker_emergency)

    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = { MapStyle("mapbox://styles/mapbox/streets-v12") },
        ) {
            if (routePoints.size >= 2) {
                PolylineAnnotation(points = routePoints) {
                    lineColor = Color(0xFF2563EB)
                    lineWidth = 4.0
                }
            }
            PointAnnotation(point = origin) {
                iconImage = originIcon
                iconSize = 0.55
            }
            PointAnnotation(point = destination) {
                iconImage = destinationIcon
                iconSize = 0.7
            }
        }
    }
}

@Composable
private fun SummaryCard(incident: Incident) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = incident.summary ?: "No additional details provided.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun DetailsCard(incident: Incident, distanceKm: Double?) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            DetailRow(Icons.Filled.Place, "Location", incident.locationName ?: incident.community)
            DetailRow(Icons.Filled.Person, "Reported by", incident.userName ?: "Anonymous")
            DetailRow(Icons.Filled.Schedule, "Reported at", incident.reportedAt)
            distanceKm?.let {
                DetailRow(Icons.Filled.Navigation, "Distance", "%.1f km away".format(it))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Grey500, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Grey500,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
