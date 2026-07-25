package com.wildwatch.app.ui.incidentdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import com.wildwatch.app.domain.model.Incident

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    onBack: () -> Unit,
    onStartGps: () -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadDistance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.incident?.species ?: "Incident") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            val incident = uiState.incident
            if (incident == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                IncidentDetailBody(incident, uiState.distanceKm, onStartGps, Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun IncidentDetailBody(
    incident: Incident,
    distanceKm: Double?,
    onStartGps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(onClick = {}, label = { Text(incident.severity.name) })
            SuggestionChip(onClick = {}, label = { Text(incident.type.name) })
        }

        MetadataGrid(incident, distanceKm, Modifier.padding(top = 16.dp))

        incident.summary?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
        }

        if (incident.evidencePhotoUrls.isNotEmpty()) {
            EvidenceGallery(incident.evidencePhotoUrls, Modifier.padding(top = 16.dp))
        }

        if (incident.voiceNoteUrl != null) {
            Text(
                text = "Voice note (${incident.voiceNoteDurationSec ?: 0}s)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Text(" Call", modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Message, contentDescription = null)
                Text(" Message", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Button(onClick = onStartGps, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("Start GPS")
        }
    }
}

@Composable
private fun MetadataGrid(incident: Incident, distanceKm: Double?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetadataRow("Location", incident.locationName ?: incident.community)
        val distanceText = distanceKm?.let { "%.1f km away".format(it) } ?: "Distance unavailable"
        MetadataRow("Distance", distanceText)
        MetadataRow("Reported", incident.reportedAt)
        MetadataRow("Reporter", incident.userName ?: "Anonymous")
        MetadataRow("Species", incident.species)
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EvidenceGallery(photoUrls: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Evidence (${photoUrls.size})", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            items(photoUrls) { url ->
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp),
                )
            }
        }
    }
}
