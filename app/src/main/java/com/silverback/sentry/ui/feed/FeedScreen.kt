package com.silverback.sentry.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.domain.model.Observation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onAddSighting: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val observations by viewModel.observations.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sightings") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSighting) {
                Icon(Icons.Filled.Add, contentDescription = "Add sighting")
            }
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (observations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sightings yet - tap + to log one", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(observations, key = { it.id }) { observation ->
                        ObservationRow(observation, onMarkAttended = { viewModel.markAttended(observation.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationRow(observation: Observation, onMarkAttended: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(observation.gorillaGroup, style = MaterialTheme.typography.titleMedium)
                if (observation.observationStatus == ObservationStatus.ATTENDED) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Attended",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = observation.locationName ?: observation.location,
                style = MaterialTheme.typography.bodyMedium,
            )
            observation.healthStatus?.let { Text("Health: $it", style = MaterialTheme.typography.bodySmall) }
            Text(
                text = "By ${observation.userName ?: "Anonymous"} - ${observation.syncStatus.name.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
            )
            if (observation.observationStatus != ObservationStatus.ATTENDED) {
                TextButton(onClick = onMarkAttended) {
                    Text("Mark Attended")
                }
            }
        }
    }
}
