package com.wildwatch.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.wildwatch.app.R
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.theme.Grey100
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey300
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.InstaBlue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIncidentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onReportSighting: () -> Unit,
    onReportConflict: () -> Unit,
    onReportCompensation: () -> Unit,
    onCommunityAlertsClick: () -> Unit,
    onOpenCommunityMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WildWatch",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = onReportSighting) {
                        Icon(Icons.Filled.AddBox, contentDescription = "New Sighting")
                    }
                    IconButton(onClick = onCommunityAlertsClick) {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = "Alerts")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // "Stories" - Active Zones / Communities
            item {
                ActiveZonesRow(uiState.zones)
                HorizontalDivider(thickness = 0.5.dp, color = Grey200)
            }

            // Feed
            if (uiState.recentReports.isEmpty()) {
                item {
                    EmptyFeedState()
                }
            } else {
                items(uiState.recentReports, key = { it.id }) { incident ->
                    FeedItem(
                        incident = incident,
                        onClick = { onIncidentClick(incident.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveZonesRow(zones: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StoryItem(label = "Your Report", isLive = false)
        }
        items(zones) { zone ->
            StoryItem(label = zone, isLive = true)
        }
    }
}

@Composable
private fun StoryItem(label: String, isLive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .then(
                    if (isLive) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(InstaBlue, Color.Cyan)),
                            shape = CircleShape
                        )
                    } else {
                        Modifier.border(1.dp, Grey300, CircleShape)
                    }
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Grey200),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (label == "Your Report") Icons.Filled.AddBox else Icons.Filled.NotificationsNone,
                contentDescription = null,
                tint = Grey500,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun FeedItem(incident: Incident, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Grey200)
            )
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    text = incident.userName ?: "Anonymous",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = incident.locationName ?: incident.community,
                    style = MaterialTheme.typography.labelSmall,
                    color = Grey500
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        // Image / Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Grey100),
            contentAlignment = Alignment.Center
        ) {
            if (incident.evidencePhotoUrls.isNotEmpty()) {
                AsyncImage(
                    model = incident.evidencePhotoUrls.first(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    incident.species,
                    style = MaterialTheme.typography.displaySmall,
                    color = Grey300,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.FavoriteBorder, contentDescription = "Verify", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = {}) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Dispatch", modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Caption
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(
                text = "${incident.severity.name} severity incident reported.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            incident.summary?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Text(
                text = relativeDay(incident.reportedAt),
                style = MaterialTheme.typography.labelSmall,
                color = Grey500,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EmptyFeedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.home_no_reports),
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun relativeDay(reportedAt: String): String {
    val instant = runCatching { Instant.parse(reportedAt) }.getOrNull() ?: return reportedAt
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
    }
}
