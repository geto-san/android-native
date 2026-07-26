package com.wildwatch.app.feature.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.Severity
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.component.StatusPill
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Info
import com.wildwatch.app.core.ui.theme.Success
import com.wildwatch.app.core.ui.theme.Warning
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangerTrackingScreen(
    onIncidentClick: (String) -> Unit,
    viewModel: RangerTrackingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Incident Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Your assigned cases",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KpiCard(Icons.Filled.CheckCircle, uiState.resolvedCount, "Resolved", Success, Modifier.weight(1f))
                KpiCard(Icons.Filled.Schedule, uiState.inProgressCount, "In progress", Warning, Modifier.weight(1f))
                KpiCard(Icons.Filled.WarningAmber, uiState.escalatedCount, "Escalated", Destructive, Modifier.weight(1f))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TrackingFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.selectFilter(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.cases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No cases match this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.cases, key = { it.id }) { incident ->
                        CaseRow(incident = incident, onClick = { onIncidentClick(incident.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(icon: ImageVector, value: Int, label: String, tone: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaseRow(incident: Incident, onClick: () -> Unit) {
    val (statusText, statusTone) = incident.statusTextAndTone()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(text = incident.severity.label(), contentColor = incident.severity.tone())
                if (incident.isEscalated) {
                    StatusPill(text = "Escalated", contentColor = Destructive)
                }
            }
            Text(
                text = "${incident.species} · ${incident.locationName ?: incident.community}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(text = statusText, contentColor = statusTone)
                Text(
                    relativeTime(incident.lastModified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Incident.statusTextAndTone(): Pair<String, Color> = when (status) {
    IncidentStatus.OPEN -> "En route" to Info
    IncidentStatus.IN_PROGRESS -> "On site" to Warning
    IncidentStatus.RESOLVED -> "Resolved" to Success
}

private fun Severity.label(): String = when (this) {
    Severity.CRITICAL -> "Urgent"
    Severity.HIGH -> "High"
    Severity.MEDIUM -> "Medium"
    Severity.LOW -> "Low"
}

private fun Severity.tone(): Color = when (this) {
    Severity.CRITICAL -> Destructive
    Severity.HIGH -> Warning
    Severity.MEDIUM -> Info
    Severity.LOW -> Success
}

private fun relativeTime(epochMillis: Long): String {
    val diffMs = System.currentTimeMillis() - epochMillis
    return when {
        diffMs < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}m"
        diffMs < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}h"
        diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d"
    }
}
