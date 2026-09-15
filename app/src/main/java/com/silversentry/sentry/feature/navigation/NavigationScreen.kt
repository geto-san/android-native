package com.silversentry.sentry.feature.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.silversentry.sentry.R
import com.silversentry.sentry.core.data.map.isMapboxTokenConfigured
import com.silversentry.sentry.core.tracking.PatrolTrackingService
import com.silversentry.sentry.core.ui.component.PermissionDialog
import com.silversentry.sentry.core.ui.theme.Destructive
import com.silversentry.sentry.core.ui.theme.White
import java.util.Locale
import kotlin.math.ceil

// The Google-Maps-style navigation view opened by START RESPONSE: a full-screen map with
// the current driving route drawn between the ranger's live position and the incident, a
// slim ETA/arrival card up top, the camera glued to the ranger as they move, and a single
// bottom STOP RESPONSE action. Entering starts patrol tracking (breadcrumbs for the park's
// response record); leaving - Stop or the system back gesture - ends the patrol.
@Composable
fun NavigationScreen(
    onBack: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    fun beginPatrolTracking() {
        context.startForegroundService(PatrolTrackingService.startIntent(context, null))
    }

    fun stopAndExit() {
        viewModel.stopFollowing()
        runCatching { context.startService(PatrolTrackingService.stopIntent(context)) }
        onBack()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            beginPatrolTracking()
            viewModel.startFollowing()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            beginPatrolTracking()
            viewModel.startFollowing()
        } else {
            showPermissionDialog = true
        }
    }

    BackHandler { stopAndExit() }

    if (showPermissionDialog) {
        PermissionDialog(
            icon = Icons.Filled.LocationOn,
            title = "Allow SilverBack Sentry to track your location?",
            description = "While you're navigating to this incident, we record your route " +
                "so the park has an accurate response record - even if you switch apps.",
            onAllow = {
                showPermissionDialog = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showPermissionDialog = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMapboxTokenConfigured() && uiState.hasValidDestination) {
            NavigationMap(uiState = uiState)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.hasValidDestination) "Map unavailable" else "Incident has no location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NavigationEtaCard(
            uiState = uiState,
            onBack = ::stopAndExit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        uiState.errorMessage?.let { message ->
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 84.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Destructive,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = Destructive,
                )
            }
        }

        Button(
            onClick = ::stopAndExit,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("STOP RESPONSE", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NavigationMap(uiState: NavigationUiState) {
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(14.2)
            uiState.userLocation?.let { center(it) }
        }
    }

    val userIcon: IconImage = rememberIconImage(R.drawable.ic_marker_pending)
    val destinationIcon: IconImage = rememberIconImage(R.drawable.ic_marker_emergency)

    // Keep the camera glued to the ranger (Google-Maps navigation behaviour) as their
    // position refreshes.
    LaunchedEffect(uiState.userLocation) {
        uiState.userLocation?.let { user ->
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(user)
                    .zoom(14.2)
                    .build(),
            )
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = viewportState,
        style = { MapStyle("mapbox://styles/mapbox/streets-v12") },
    ) {
        if (uiState.routePoints.size >= 2) {
            PolylineAnnotation(points = uiState.routePoints) {
                lineColor = Color(0xFF2563EB)
                lineWidth = 5.0
            }
        }
        uiState.userLocation?.let { user ->
            PointAnnotation(point = user) {
                iconImage = userIcon
                iconSize = 0.6
            }
        }
        uiState.destination?.let { destination ->
            PointAnnotation(point = destination) {
                iconImage = destinationIcon
                iconSize = 0.7
            }
        }
    }
}

@Composable
private fun NavigationEtaCard(
    uiState: NavigationUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "End navigation",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Navigate to ${uiState.destinationName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 1,
                )
                uiState.destinationPlace.takeIf { it.isNotBlank() }?.let { place ->
                    Text(
                        text = place,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = formatEta(uiState.durationSeconds, uiState.distanceKm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
                Text(
                    text = formatDistanceKm(uiState.distanceKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun formatEta(durationSeconds: Double?, distanceKm: Double?): String {
    val etaMinutes = durationSeconds?.let { ceil(it / 60.0).toInt() }
    return when {
        etaMinutes != null && etaMinutes < 1 -> "<1 min"
        etaMinutes != null -> "~$etaMinutes min"
        distanceKm != null -> "~${ceil(distanceKm / 5.0).toInt().coerceAtLeast(1)} min"
        else -> "…"
    }
}

private fun formatDistanceKm(distanceKm: Double?): String = when {
    distanceKm == null -> ""
    distanceKm < 1.0 -> "%.0f m".format(Locale.US, distanceKm * 1000.0)
    else -> "%.1f km".format(Locale.US, distanceKm)
}
