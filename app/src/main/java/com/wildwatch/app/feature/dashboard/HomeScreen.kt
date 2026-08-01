package com.wildwatch.app.feature.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.model.Article
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.ui.component.IncidentListItem
import com.wildwatch.app.core.ui.component.QuickReportCard
import com.wildwatch.app.core.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIncidentClick: (String) -> Unit,
    onReportIncident: () -> Unit,
    onEditDraft: (String, com.wildwatch.app.core.database.IncidentType) -> Unit,
    onNotificationsClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "WildWatch",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = MagilioFontFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
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
                
                if (uiState.pendingUploadsCount > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.triggerSync() },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "${uiState.pendingUploadsCount} reports pending upload. Tap to sync.",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 8.dp).weight(1f)
                            )
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Community Alerts Card
            if (uiState.communityAlerts.isNotEmpty()) {
                item {
                    CommunityAlertsCard(
                        alerts = uiState.communityAlerts,
                        parkName = uiState.park
                    )
                }
            }

            // Drafts Section
            if (uiState.drafts.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Unfinished Drafts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.drafts.forEach { draft ->
                                QuickReportCard(
                                    title = draft.species.ifBlank { "New Draft" },
                                    icon = if (draft.type == com.wildwatch.app.core.database.IncidentType.SIGHTING) Icons.Filled.AddAPhoto else Icons.Filled.Warning,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { onEditDraft(draft.id, draft.type) },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Report Section - Consolidated
            item {
                Column {
                    Text(
                        text = "Quick report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ReportIncidentCard(
                        onClick = onReportIncident
                    )
                }
            }

            // Recent Reports Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My recent reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onSeeAllClick() }
                        )
                    }
                    
                    if (uiState.recentReports.isEmpty()) {
                        EmptyFeedState()
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.recentReports.forEach { incident ->
                                IncidentListItem(
                                    title = incident.species,
                                    subtitle = "${relativeDay(incident.reportedAt)} · ${incident.locationName ?: incident.community}",
                                    status = incident.status.name,
                                    statusColor = if (incident.status.name == "RESOLVED") Success else Warning,
                                    icon = if (incident.type == com.wildwatch.app.core.database.IncidentType.SIGHTING) Icons.Filled.AddAPhoto else Icons.Filled.Warning,
                                    iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    onClick = { onIncidentClick(incident.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Feed / News Section
            if (uiState.newsArticles.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Latest News",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.newsArticles.forEach { article ->
                                HomeArticleCard(
                                    article = article,
                                    onClick = { onArticleClick(article.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportIncidentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Report Incident",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sighting or Conflict",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HomeArticleCard(
    article: Article,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.source.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = article.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CommunityAlertsCard(
    alerts: List<Incident>,
    parkName: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedAlertId by remember { mutableStateOf<String?>(null) }

    // Reset selected alert when collapsing
    LaunchedEffect(isExpanded) {
        if (!isExpanded) selectedAlertId = null
    }

    val selectedAlert = remember(selectedAlertId, alerts) {
        alerts.find { it.id == selectedAlertId }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.NotificationsOff else Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (isExpanded) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isExpanded) "Active Alerts Details" else "Community Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "Tap to collapse" else "${alerts.size} active alerts in $parkName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alerts.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                when {
                    alerts.size == 1 -> {
                        AlertDetailContent(alerts.first())
                    }
                    selectedAlert != null -> {
                        Column {
                            TextButton(
                                onClick = { selectedAlertId = null },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back to list", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AlertDetailContent(selectedAlert)
                        }
                    }
                    else -> {
                        AlertListContent(alerts) { selectedAlertId = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertDetailContent(incident: Incident) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailRow("Species", incident.species, Icons.Filled.Pets)
        DetailRow("Location", incident.locationName ?: incident.community, Icons.Filled.LocationOn)
        incident.summary?.let {
            DetailRow("Summary", it, Icons.Filled.Notes)
        }
        if (incident.evidencePhotoUrls.isNotEmpty()) {
            Text(
                text = "Photos (${incident.evidencePhotoUrls.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(incident.evidencePhotoUrls.size.coerceAtMost(3)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertListContent(alerts: List<Incident>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(alert.id) },
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (alert.type == com.wildwatch.app.core.database.IncidentType.SIGHTING) Icons.Filled.AddAPhoto else Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(alert.species, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(alert.community, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyFeedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Activity Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When you report sightings or conflicts, they'll appear here to help the community stay informed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
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
