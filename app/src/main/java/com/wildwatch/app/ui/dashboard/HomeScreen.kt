package com.wildwatch.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.res.stringResource
import com.wildwatch.app.R
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.domain.model.Incident
import com.wildwatch.app.ui.components.CountBadge
import com.wildwatch.app.ui.components.GradientHeader
import com.wildwatch.app.ui.components.IconBadge
import com.wildwatch.app.ui.components.InitialsAvatar
import com.wildwatch.app.ui.components.StatusPill
import com.wildwatch.app.ui.map.MapViewModel
import com.wildwatch.app.ui.map.markerHueFor
import com.wildwatch.app.ui.theme.Cream
import com.wildwatch.app.ui.theme.Destructive
import com.wildwatch.app.ui.theme.Info
import com.wildwatch.app.ui.theme.Success
import com.wildwatch.app.ui.theme.SunsetAmber
import com.wildwatch.app.ui.theme.Warning as WarningColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BWINDI_CENTER = LatLng(-1.03, 29.66)

// wireframes 5 & 5b: the Home tab.
@Composable
fun HomeScreen(
    onIncidentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onReportSighting: () -> Unit,
    onReportConflict: () -> Unit,
    onReportCompensation: () -> Unit,
    onCommunityAlertsClick: () -> Unit,
    onOpenCommunityMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapUiState by mapViewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GradientHeader(bottomCornerRadius = 28.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clickable(onClick = onProfileClick)) {
                            InitialsAvatar(initials = initialsFor(uiState.displayName))
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                stringResource(R.string.home_welcome_back, uiState.displayName.substringBefore(' ')),
                                style = MaterialTheme.typography.titleLarge,
                                color = Cream,
                            )
                            Text(
                                "${uiState.park} · ${uiState.language}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Cream.copy(alpha = 0.8f),
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(
                                icon = Icons.Filled.Forest,
                                background = SunsetAmber.copy(alpha = 0.18f),
                                tint = SunsetAmber,
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    stringResource(R.string.home_community_impact),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.home_reports_resolved, uiState.reportsThisMonth, uiState.resolvedThisMonth),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            StatusPill(text = "+18%", contentColor = Success)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onCommunityAlertsClick),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(icon = Icons.Filled.Campaign, background = Info.copy(alpha = 0.15f), tint = Info)
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(stringResource(R.string.home_community_alerts), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.home_new_alerts_in_park, uiState.unreadAlertCount, uiState.park),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        CountBadge(count = uiState.unreadAlertCount)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    stringResource(R.string.home_quick_report),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickReportTile(
                        icon = Icons.Filled.CameraAlt,
                        label = stringResource(R.string.home_wildlife_sighting),
                        background = MaterialTheme.colorScheme.primary,
                        onClick = onReportSighting,
                        modifier = Modifier.weight(1f),
                    )
                    QuickReportTile(
                        icon = Icons.Filled.Warning,
                        label = stringResource(R.string.home_conflict_report),
                        background = Destructive,
                        onClick = onReportConflict,
                        modifier = Modifier.weight(1f),
                    )
                    QuickReportTile(
                        icon = Icons.Filled.Receipt,
                        label = stringResource(R.string.home_compensation_claim),
                        background = SunsetAmber,
                        onClick = onReportCompensation,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                ) {
                    Text(stringResource(R.string.home_recent_reports), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.home_see_all), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                if (uiState.recentReports.isEmpty()) {
                    Text(
                        stringResource(R.string.home_no_reports),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.recentReports.forEach { incident ->
                        RecentReportRow(incident, onClick = { onIncidentClick(incident.id) })
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                icon = Icons.Filled.LocationOn,
                                background = MaterialTheme.colorScheme.secondary,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 36.dp,
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(stringResource(R.string.home_nearby_alert), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Elephant herd movement — 3.2km west",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        CommunityMapPreview(
                            incidents = mapUiState.incidents,
                            onClick = onOpenCommunityMap,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

// A non-interactive map snapshot (all gestures disabled so it doesn't fight
// the Home screen's own vertical scroll) that opens the full CommunityMapScreen
// on tap - replaces the static gradient placeholder wireframe 5b showed here.
@Composable
private fun CommunityMapPreview(incidents: List<Incident>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BWINDI_CENTER, 11f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = com.google.maps.android.compose.MapType.NORMAL),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                myLocationButtonEnabled = false,
            ),
        ) {
            for (incident in incidents) {
                Marker(
                    state = remember(incident.id) { MarkerState(position = LatLng(incident.lat, incident.lng)) },
                    title = incident.species,
                    icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(incident.severity)),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text(
                stringResource(R.string.home_live_map),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun QuickReportTile(
    icon: ImageVector,
    label: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .background(background, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Cream, modifier = Modifier.align(Alignment.TopStart).size(22.dp))
        Text(
            label,
            color = Cream,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 3,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun RecentReportRow(incident: Incident, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = incidentIcon(incident),
                background = MaterialTheme.colorScheme.secondary,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 40.dp,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(incident.species, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${relativeDay(incident.reportedAt)} · ${incident.locationName ?: incident.community}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val (label, color) = reportStatusLabel(incident)
            StatusPill(text = label, contentColor = color)
        }
    }
}

private fun incidentIcon(incident: Incident): ImageVector = when {
    incident.isEscalated -> Icons.Filled.Warning
    else -> Icons.Filled.Forest
}

private fun reportStatusLabel(incident: Incident): Pair<String, Color> = when {
    incident.isEscalated -> "Escalated" to Destructive
    incident.status == IncidentStatus.RESOLVED -> "Confirmed" to Success
    else -> "In Review" to WarningColor
}

private fun relativeDay(reportedAt: String): String {
    val instant = runCatching { Instant.parse(reportedAt) }.getOrNull() ?: return reportedAt
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}

private fun initialsFor(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "?" }
