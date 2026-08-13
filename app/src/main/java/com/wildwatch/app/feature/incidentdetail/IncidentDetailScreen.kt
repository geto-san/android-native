package com.wildwatch.app.feature.incidentdetail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.tracking.PatrolTrackingService
import com.wildwatch.app.core.ui.component.IconBadge
import com.wildwatch.app.core.ui.component.PermissionDialog
import com.wildwatch.app.core.ui.component.StatusPill
import com.wildwatch.app.core.ui.component.severityColor
import com.wildwatch.app.core.ui.component.statusColor
import com.wildwatch.app.core.ui.component.statusLabel
import com.wildwatch.app.core.ui.component.typeIcon
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Grey500

// Redesigned around a single primary action instead of the old three-branch assign/track/
// nothing block: a ranger who can act on this incident always sees one "Start Response"
// button, pinned to the bottom so it's reachable regardless of scroll position (the pattern
// popular trip/delivery-detail screens - Uber, DoorDash - use for their one must-not-miss
// CTA). Tapping it claims the incident (if not already the ranger's) and starts real GPS
// tracking - see IncidentDetailViewModel.respondToIncident() and PatrolTrackingService.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    onBack: () -> Unit,
    onStartGps: () -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadDistance() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    fun beginTracking() {
        context.startForegroundService(PatrolTrackingService.startIntent(context, null))
        onStartGps()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
        if (granted) beginTracking()
    }

    fun respond() {
        viewModel.respondToIncident()
        if (hasLocationPermission) {
            beginTracking()
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(
            icon = Icons.Filled.LocationOn,
            title = "Allow WildWatch to track your location?",
            description = "While you're responding, we record your route so the park has an " +
                "accurate record of your response - even if you switch apps.",
            onAllow = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showPermissionDialog = false },
        )
    }

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
                        onClick = ::respond,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isAssignedToMe) "Resume Response" else "Start Response",
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
                    text = incident.species,
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
                DetailRow(Icons.Filled.NearMe, "Distance", "%.1f km away".format(it))
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
