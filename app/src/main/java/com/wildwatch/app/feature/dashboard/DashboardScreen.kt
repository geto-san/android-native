package com.wildwatch.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.R
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.component.CountBadge
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.MagilioFontFamily
import com.wildwatch.app.core.ui.theme.SunsetAmber
import com.wildwatch.app.core.util.relativeDay

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
                        "Professional Dashboard", 
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                StatsSection(uiState)
                HorizontalDivider(thickness = 0.5.dp, color = Grey200)
            }

            item {
                ZoneFilterChips(
                    zones = uiState.zones,
                    selectedZone = uiState.selectedZone,
                    onSelectZone = viewModel::selectZone,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            item {
                SectionHeader(stringResource(R.string.dash_active_incidents))
            }

            if (uiState.activeIncidents.isEmpty()) {
                item {
                    EmptyState("Nothing currently being worked")
                }
            } else {
                items(uiState.activeIncidents, key = { "incident_${it.id}" }) { incident ->
                    DashboardIncidentItem(
                        incident = incident,
                        actionLabel = stringResource(R.string.dash_attend_to),
                        onAction = { onIncidentClick(incident.id) },
                        onAssign = null
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.dash_active_alerts))
            }

            if (uiState.activeAlerts.isEmpty()) {
                item {
                    EmptyState("No unassigned alerts")
                }
            } else {
                items(uiState.activeAlerts, key = { "alert_${it.id}" }) { incident ->
                    DashboardIncidentItem(
                        incident = incident,
                        actionLabel = stringResource(R.string.dash_attend_to),
                        onAction = { onIncidentClick(incident.id) },
                        // Alerts are unassigned by definition (DashboardViewModel filters on
                        // assignedTo == null) - assignToSelf() already existed on the
                        // ViewModel but had no button anywhere to reach it from here.
                        onAssign = { viewModel.assignToSelf(incident.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(uiState: DashboardUiState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Account Insights",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Last 30 days",
            style = MaterialTheme.typography.labelSmall,
            color = Grey500
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(
                label = "Resolved",
                value = uiState.resolvedCount.toString(),
                icon = Icons.Filled.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Pending",
                value = uiState.pendingCount.toString(),
                icon = Icons.Filled.Schedule,
                tint = SunsetAmber,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Zones",
                value = uiState.activeZoneCount.toString(),
                icon = Icons.Filled.Map,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Grey500)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

private fun typeIcon(type: IncidentType): ImageVector = when (type) {
    IncidentType.SIGHTING -> Icons.Filled.Visibility
    IncidentType.CONFLICT -> Icons.Filled.Groups
    IncidentType.EMERGENCY -> Icons.Filled.ReportProblem
    IncidentType.POACHING -> Icons.Filled.Dangerous
    IncidentType.SNARE -> Icons.Filled.Warning
}

// Matches the severity/color convention already established by SeverityChip
// in CommonReportComponents.kt - same three brand colors, no new ones.
@Composable
private fun severityColor(severity: IncidentSeverity): Color = when (severity) {
    IncidentSeverity.MEDIUM -> SunsetAmber
    IncidentSeverity.HIGH -> Destructive
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun DashboardIncidentItem(
    incident: Incident,
    actionLabel: String,
    onAction: () -> Unit,
    onAssign: (() -> Unit)?
) {
    val accent = severityColor(incident.severity)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onAction)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon(incident.type), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = incident.species,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (incident.isEscalated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "Escalated",
                            tint = Destructive,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${incident.locationName ?: incident.community} · ${relativeDay(incident.reportedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey500
                )
            }
            SeverityLabel(severity = incident.severity, color = accent)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (onAssign != null) {
                Button(
                    onClick = onAssign,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.size(height = 32.dp, width = 100.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign me", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(height = 32.dp, width = 100.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SeverityLabel(severity: IncidentSeverity, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = severity.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ZoneFilterChips(
    zones: List<String>,
    selectedZone: String?,
    onSelectZone: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            FilterChip(
                selected = selectedZone == null,
                onClick = { onSelectZone(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
        items(zones) { zone ->
            FilterChip(
                selected = selectedZone == zone,
                onClick = { onSelectZone(zone) },
                label = { Text(zone) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = Grey500)
    }
}
