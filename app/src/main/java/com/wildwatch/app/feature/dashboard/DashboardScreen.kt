package com.wildwatch.app.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.R
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.MagilioFontFamily

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
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
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
                        onAction = { onIncidentClick(incident.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Grey200, modifier = Modifier.padding(start = 16.dp))
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
                        actionLabel = stringResource(R.string.dash_assign_to_me),
                        onAction = { viewModel.assignToSelf(incident.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Grey200, modifier = Modifier.padding(start = 16.dp))
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Resolved", uiState.resolvedCount.toString())
            StatItem("Pending", uiState.pendingCount.toString())
            StatItem("Zones", uiState.activeZoneCount.toString())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
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

@Composable
private fun DashboardIncidentItem(
    incident: Incident,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = incident.species, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = incident.locationName ?: incident.community,
                style = MaterialTheme.typography.bodySmall,
                color = Grey500
            )
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
            Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
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
