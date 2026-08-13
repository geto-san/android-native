package com.wildwatch.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.R
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.component.CountBadge
import com.wildwatch.app.core.ui.component.IconBadge
import com.wildwatch.app.core.ui.component.StatusPill
import com.wildwatch.app.core.ui.component.severityColor
import com.wildwatch.app.core.ui.component.statusColor
import com.wildwatch.app.core.ui.component.statusLabel
import com.wildwatch.app.core.ui.component.typeIcon
import com.wildwatch.app.core.ui.component.typeLabel
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Grey300
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.MagilioFontFamily
import com.wildwatch.app.core.util.relativeDay

// Every card here is tap-only (no per-card buttons) - severity is read from the leading
// accent bar and status from the trailing pill, so the information a button-driven design
// used to require reading a label for is now visible before you even tap in. "Respond"
// (claim + start tracking) now lives entirely on the incident detail screen, reached by
// tapping a card - see IncidentDetailScreen/IncidentDetailViewModel.respondToIncident().
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onIncidentClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.dash_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MagilioFontFamily
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        if (uiState.unreadNotificationCount > 0) {
                            BadgedBox(badge = { CountBadge(uiState.unreadNotificationCount) }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                        } else {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ZoneDropdown(
                    zones = uiState.zones,
                    selectedZone = uiState.selectedZone,
                    onSelectZone = viewModel::selectZone,
                )
            }

            if (uiState.activeIncidents.isEmpty() && uiState.activeAlerts.isEmpty()) {
                item { EmptyState() }
            } else {
                items(uiState.activeIncidents, key = { "incident_${it.id}" }) { incident ->
                    IncidentCard(incident = incident, onClick = { onIncidentClick(incident.id) })
                }
                items(uiState.activeAlerts, key = { "alert_${it.id}" }) { incident ->
                    IncidentCard(incident = incident, onClick = { onIncidentClick(incident.id) })
                }
            }
        }
    }
}

// One card design for both the in-progress and unclaimed lists - a colored leading edge
// carries severity, a trailing pill carries status (which also tells the two lists apart,
// now that there's no header text between them), and a chevron is the only "action"
// affordance, matching the tap-only pattern IncidentListItem already established elsewhere
// in the app (Home's "My recent reports").
@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    val accent = severityColor(incident.severity)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(accent)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(
                    icon = typeIcon(incident.type),
                    background = accent.copy(alpha = 0.12f),
                    tint = accent,
                    size = 44.dp,
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLabel(incident.type).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Grey500,
                            fontWeight = FontWeight.Bold,
                        )
                        if (incident.isEscalated) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = "Escalated",
                                tint = Destructive,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = incident.species,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Grey500,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${incident.locationName ?: incident.community} · ${relativeDay(incident.reportedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Grey500,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(
                        text = statusLabel(incident.status),
                        contentColor = statusColor(incident.status),
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Grey300,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneDropdown(
    zones: List<String>,
    selectedZone: String?,
    onSelectZone: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "ZoneDropdownChevron")
    val allZonesLabel = stringResource(R.string.dash_all_zones)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedZone ?: allZonesLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allZonesLabel) },
                onClick = { onSelectZone(null); expanded = false },
                trailingIcon = {
                    if (selectedZone == null) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            zones.forEach { zone ->
                DropdownMenuItem(
                    text = { Text(zone) },
                    onClick = { onSelectZone(zone); expanded = false },
                    trailingIcon = {
                        if (selectedZone == zone) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "All clear",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Nothing needs a response right now.",
                style = MaterialTheme.typography.bodySmall,
                color = Grey500,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
