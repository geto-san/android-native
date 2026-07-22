package com.silverback.sentry.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userDisplayName: String?,
    onViewSightings: () -> Unit,
    onAddSighting: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, ${userDisplayName ?: "Ranger"}") },
                actions = { TextButton(onClick = onSignOut) { Text("Sign Out") } },
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
                    text = if (uiState.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard("Total", uiState.totalCount, Modifier.weight(1f))
                    StatCard("Pending Sync", uiState.pendingSyncCount, Modifier.weight(1f))
                    StatCard("Attended", uiState.attendedCount, Modifier.weight(1f))
                }

                Text(
                    text = "${uiState.needsAttentionCount} sightings need attention",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )

                Button(onClick = onViewSightings, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text("View Sightings")
                }
                Button(onClick = onAddSighting, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Add Sighting")
                }
            }
        }
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
