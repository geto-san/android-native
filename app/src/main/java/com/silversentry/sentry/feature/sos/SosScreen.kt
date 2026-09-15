package com.silversentry.sentry.feature.sos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.silversentry.sentry.core.data.map.isMapboxTokenConfigured
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.ui.component.PermissionDialog
import com.silversentry.sentry.feature.report.ReportIncidentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PrimaryRed = Color(0xFFD32F2F)
private val DarkText = Color(0xFF1A1A1A)
private val MutedText = Color(0xFF4A4A4A)
private val MapBackdrop = Color(0xFFE8E6DF)

private const val PULSE_DURATION_MS = 2600
private const val CANCEL_HOLD_MS = 3000L
private const val SEND_BACKSTOP_MS = 8000L

// Dedicated emergency flow launched from the raised SOS button floating over the main tab
// screens. Matches the reference SOS-alert screens: a live map pinned to the reporter's
// position, concentric pulsing radar rings from a central red badge, a white status header
// up top, and a red-gradient action panel below whose only control is a 3-second
// long-press "HOLD TO CANCEL SOS" (panic-resistant - a real alert is never cancelled by a
// stray tap). The SOS incident is created automatically on entry; the button press was the
// intent, so there is no confirm dialog. GPS/park/timestamp are auto-captured by
// ReportIncidentViewModel (preset to IncidentType.SOS). A completed hold withdraws the
// alert end-to-end (CANCELLED status synced to Firestore) so responders - the ranger map,
// the community stream, and the web portal - all see it turned off.
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(15.0)
            center(Point.fromLngLat(29.66, -1.03))
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadCurrentLocation()
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(null, presetType = IncidentType.SOS)
        if (!hasLocationPermission) {
            showLocationPermissionDialog = true
        }
    }

    // Sending is automatic (see the file header) but must not fire before GPS settles -
    // the SOS is pinned to the captured coordinates. Send as soon as location resolution
    // succeeds OR fails (location is best-effort; the report is still submittable), with a
    // worst-case backstop so a hung location fetch never blocks an emergency forever.
    LaunchedEffect(uiState.locationName, uiState.locationError) {
        val locationSettled = uiState.locationName != null || uiState.locationError != null
        if (!sent && !uiState.isSaving && locationSettled) {
            sent = true
            viewModel.save()
        }
    }
    LaunchedEffect(Unit) {
        delay(SEND_BACKSTOP_MS)
        if (!sent) {
            sent = true
            viewModel.save()
        }
    }

    LaunchedEffect(uiState.lat, uiState.lng) {
        if (uiState.lat != 0.0 || uiState.lng != 0.0) {
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(uiState.lng, uiState.lat))
                    .zoom(15.0)
                    .build(),
            )
        }
    }

    if (showLocationPermissionDialog) {
        PermissionDialog(
            icon = Icons.Filled.Warning,
            title = "Allow SilverBack Sentry to use your location?",
            description = "Your exact location is pinned on the map and sent to responders with this SOS.",
            onAllow = {
                showLocationPermissionDialog = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showLocationPermissionDialog = false },
        )
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map background pinned to the reporter's current location.
        if (remember { isMapboxTokenConfigured() }) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewportState,
                style = { MapStyle("mapbox://styles/mapbox/streets-v12") },
            ) {}
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MapBackdrop))
        }

        // 2. Bottom red-gradient overlay (from the crash-alert reference) meeting the
        // central radar, housing the live status + action panel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PrimaryRed.copy(alpha = 0.55f),
                            PrimaryRed.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )

        // 3. Central pulsing radar radiating from the red SOS badge.
        Box(modifier = Modifier.align(Alignment.Center)) {
            SosRadar(modifier = Modifier.align(Alignment.Center))
            SosBadge(modifier = Modifier.align(Alignment.Center))
        }

        // 4. Top status header card.
        SosHeaderCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        )

        // 5. Bottom action panel.
        SosActionPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
            saveError = uiState.saveError,
            // A completed hold is a real withdrawal: the alert is marked CANCELLED locally and
            // re-queued for sync so it turns off on the ranger map, the community stream, and
            // the web portal - not just on this screen.
            onCancelSos = { viewModel.cancelSos(onBack) },
        )
    }
}

// Concentric red radar pulses expanding outward over the map, simulating the alert
// broadcast range. Three rings with offset phases so the sweep never goes dark.
@Composable
private fun SosRadar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sos_radar")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sos_radar_phase",
    )

    Canvas(modifier = modifier.size(360.dp)) {
        val maxRadius = size.minDimension / 2f
        val strokeWidth = 8.dp.toPx()
        drawCircle(color = PrimaryRed.copy(alpha = 0.06f), radius = maxRadius * 0.28f)

        for (i in 0..2) {
            val offsetProgress = (phase + i * 0.33f) % 1f
            val radius = maxRadius * offsetProgress
            val alpha = (1f - offsetProgress) * 0.5f
            drawCircle(
                color = PrimaryRed,
                radius = radius,
                alpha = alpha,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
private fun SosBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        color = PrimaryRed,
        border = BorderStroke(4.dp, Color.White),
        shadowElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun SosHeaderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "WILDLIFE SOS ACTIVE",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkText,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UWA Rangers & Community Patrols Notified",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SosActionPanel(
    modifier: Modifier = Modifier,
    saveError: String?,
    onCancelSos: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        saveError?.let { error ->
            Text(
                text = error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        Text(
            text = "Broadcasting your live location to response teams...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(18.dp))

        HoldToCancelButton(
            onCancel = onCancelSos,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        )
    }
}

// The cancel control is deliberately a 3-second hold (not a tap): a real emergency alert
// must never be cancelled by an accidental touch, and the fill animation doubles as the
// confirmation. Uses rememberCoroutineScope so the hold can be cancelled the moment the
// finger lifts.
@Composable
private fun HoldToCancelButton(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animateProgress = remember { Animatable(0f) }
    val localScope = rememberCoroutineScope()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val job = localScope.launch {
                            animateProgress.snapTo(0f)
                            animateProgress.animateTo(1f, tween(durationMillis = CANCEL_HOLD_MS.toInt()))
                            onCancel()
                        }
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                        }
                        job.cancel()
                        localScope.launch { animateProgress.snapTo(0f) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animateProgress.value)
                    .height(64.dp)
                    .background(PrimaryRed.copy(alpha = 0.14f)),
            )
            Text(
                text = "HOLD TO CANCEL SOS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = PrimaryRed,
            )
        }
    }
}
