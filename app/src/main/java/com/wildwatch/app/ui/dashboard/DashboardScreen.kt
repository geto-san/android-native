package com.wildwatch.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.wildwatch.app.R
import com.wildwatch.app.domain.model.Incident

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onIncidentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                },
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val statusColor = if (uiState.isOnline) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(
                    text = if (uiState.isOnline) stringResource(R.string.dash_online) else stringResource(R.string.dash_offline),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )

                HeroStats(uiState, modifier = Modifier.padding(top = 16.dp))

                ZoneFilterChips(
                    zones = uiState.zones,
                    selectedZone = uiState.selectedZone,
                    onSelectZone = viewModel::selectZone,
                    modifier = Modifier.padding(top = 16.dp),
                )

                IncidentSection(
                    title = stringResource(R.string.dash_active_incidents),
                    incidents = uiState.activeIncidents,
                    actionLabel = stringResource(R.string.dash_attend_to),
                    onAction = onIncidentClick,
                    emptyMessage = "Nothing currently being worked",
                    modifier = Modifier.padding(top = 24.dp),
                )

                IncidentSection(
                    title = stringResource(R.string.dash_active_alerts),
                    incidents = uiState.activeAlerts,
                    actionLabel = stringResource(R.string.dash_assign_to_me),
                    onAction = viewModel::assignToSelf,
                    emptyMessage = "No unassigned alerts",
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroStats(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(stringResource(R.string.dash_resolved), uiState.resolvedCount, Modifier.weight(1f))
        StatCard(stringResource(R.string.dash_pending), uiState.pendingCount, Modifier.weight(1f))
        StatCard(stringResource(R.string.dash_active_zones), uiState.activeZoneCount, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ZoneFilterChips(
    zones: List<String>,
    selectedZone: String?,
    onSelectZone: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item {
            FilterChip(selected = selectedZone == null, onClick = { onSelectZone(null) }, label = { Text("All") })
        }
        items(zones) { zone ->
            FilterChip(selected = selectedZone == zone, onClick = { onSelectZone(zone) }, label = { Text(zone) })
        }
    }
}

@Composable
private fun IncidentSection(
    title: String,
    incidents: List<Incident>,
    actionLabel: String,
    onAction: (String) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (incidents.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                incidents.forEach { incident ->
                    IncidentRow(incident, actionLabel, onAction, Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(
    incident: Incident,
    actionLabel: String,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = incident.species, style = MaterialTheme.typography.titleSmall)
            Text(
                text = incident.locationName ?: incident.community,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${incident.severity.name.lowercase().replaceFirstChar(Char::uppercase)} severity",
                style = MaterialTheme.typography.labelSmall,
            )
            TextButton(onClick = { onAction(incident.id) }, modifier = Modifier.padding(top = 4.dp)) {
                Text(actionLabel)
            }
        }
    }
}
