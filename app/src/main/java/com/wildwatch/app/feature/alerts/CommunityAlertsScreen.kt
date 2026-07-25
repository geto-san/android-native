package com.wildwatch.app.feature.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.AlertCategory
import com.wildwatch.app.core.database.AlertSeverity
import com.wildwatch.app.core.model.Alert
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Info
import com.wildwatch.app.core.ui.theme.SunsetAmber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private data class CategoryFilter(val label: String, val category: AlertCategory?)

private val CATEGORY_FILTERS = listOf(
    CategoryFilter("All", null),
    CategoryFilter("Wildlife", AlertCategory.WILDLIFE),
    CategoryFilter("Safety", AlertCategory.SAFETY),
    CategoryFilter("Patrols", AlertCategory.PATROLS),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityAlertsScreen(onBack: () -> Unit, viewModel: AlertViewModel = hiltViewModel()) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(CATEGORY_FILTERS.first()) }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BackHeader(
                    title = "Notifications",
                    onBack = onBack,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}, modifier = Modifier.padding(end = 12.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(CATEGORY_FILTERS) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = null,
                        shape = CircleShape
                    )
                }
            }

            val filtered = selectedFilter.category?.let { category -> alerts.filter { it.category == category } } ?: alerts

            if (filtered.isEmpty()) {
                EmptyAlertsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { alert ->
                        AlertItem(alert)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(alert: Alert) {
    val severityColor = when (alert.severity) {
        AlertSeverity.URGENT -> Destructive
        AlertSeverity.CAUTION -> SunsetAmber
        AlertSeverity.INFO -> Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Colored dot / icon indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .padding(top = 4.dp)
                .background(severityColor, CircleShape)
        )

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        alert.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    relativeTime(alert.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    "Mark as done",
                    color = Info,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    "Update",
                    color = Info,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun EmptyAlertsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "You're all caught up",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Come back later for Reminders, health tips, moments and weight notifications",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun relativeTime(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - createdAt
    return when {
        diffMs < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)} min"
        diffMs < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)} hr"
        diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> {
            val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}
