package com.wildwatch.app.ui.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.RangerProgress
import com.wildwatch.app.domain.model.Incident

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(viewModel: TrackingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tracking") }) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                FilterChipsRow(uiState.selectedFilter, viewModel::selectFilter)

                if (uiState.incidents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No assigned incidents match this filter", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        uiState.incidents.forEach { incident ->
                            TrackingRow(incident, Modifier.padding(bottom = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun filterLabel(filter: TrackingFilter): String = filter.name
    .split("_")
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

@Composable
private fun FilterChipsRow(selected: TrackingFilter, onSelect: (TrackingFilter) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(TrackingFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filterLabel(filter)) },
            )
        }
    }
}

@Composable
private fun TrackingRow(incident: Incident, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = incident.species, style = MaterialTheme.typography.titleSmall)
            Text(
                text = incident.locationName ?: incident.community,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(onClick = {}, label = { Text(statusPillText(incident)) })
                if (incident.isEscalated) {
                    SuggestionChip(onClick = {}, label = { Text("Escalated") })
                }
            }
        }
    }
}

// RESOLVED shows the status directly; IN_PROGRESS shows the ranger's own
// operational sub-state instead - the two vocabularies don't nest (see
// IncidentStatus/RangerProgress's orthogonality), so this reads whichever
// one is actually meaningful for the current status.
private fun statusPillText(incident: Incident): String = when (incident.status) {
    IncidentStatus.RESOLVED -> "Resolved"
    IncidentStatus.IN_PROGRESS -> when (incident.rangerProgress) {
        RangerProgress.ON_SITE -> "On site"
        RangerProgress.EN_ROUTE, null -> "En route"
    }
    IncidentStatus.OPEN -> "Open"
}
