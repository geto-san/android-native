package com.wildwatch.app.feature.incidentdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.WildWatchTheme

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
                title = { 
                    Text(
                        "Incident Details", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            val incident = uiState.incident
            if (incident == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        IncidentHeader(incident)
                    }
                    
                    if (incident.evidencePhotoUrls.isNotEmpty()) {
                        item {
                            EvidenceGallery(incident.evidencePhotoUrls)
                        }
                    }
                    
                    item {
                        IncidentDescription(incident)
                    }
                    
                    item {
                        MetadataSection(incident, uiState.distanceKm)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        when {
                            uiState.canAssignToSelf -> Button(
                                onClick = { viewModel.assignToSelf() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(48.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text("Assign to me", fontWeight = FontWeight.Bold)
                            }
                            uiState.isAssignedToMe -> Button(
                                onClick = onStartGps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(48.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text("Start GPS Tracking", fontWeight = FontWeight.Bold)
                            }
                            // Community view (or a ranger viewing someone else's
                            // assignment) stays read-only - no dispatch action.
                            else -> Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentHeader(incident: Incident) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Grey200)
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = incident.species,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${incident.type.name} · ${incident.severity.name} Severity",
                style = MaterialTheme.typography.labelSmall,
                color = Grey500
            )
        }
    }
}

@Composable
private fun EvidenceGallery(photoUrls: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        items(photoUrls) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(Grey200),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun IncidentDescription(incident: Incident) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = incident.summary ?: "No additional details provided.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun MetadataSection(incident: Incident, distanceKm: Double?) {
    Column(modifier = Modifier.padding(16.dp)) {
        HorizontalDivider(thickness = 0.5.dp, color = Grey200)
        Spacer(modifier = Modifier.height(16.dp))
        
        MetadataRow("Location", incident.locationName ?: incident.community)
        MetadataRow("Reported by", incident.userName ?: "Anonymous")
        MetadataRow("Reported at", incident.reportedAt)
        
        distanceKm?.let {
            MetadataRow("Distance", "%.1f km away".format(it))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Grey500)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
