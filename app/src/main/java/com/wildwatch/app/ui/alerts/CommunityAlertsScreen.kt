package com.wildwatch.app.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.data.local.db.AlertCategory
import com.wildwatch.app.data.local.db.AlertSeverity
import com.wildwatch.app.domain.model.Alert
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.IconBadge
import com.wildwatch.app.ui.components.StatusPill
import com.wildwatch.app.ui.theme.Destructive
import com.wildwatch.app.ui.theme.Info
import com.wildwatch.app.ui.theme.SunsetAmber
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
    CategoryFilter("Trapping", AlertCategory.TRAPPING),
)

// wireframe 8 - now reading from AlertViewModel/AlertRepository instead of a
// hardcoded list.
@Composable
fun CommunityAlertsScreen(onBack: () -> Unit, viewModel: AlertViewModel = hiltViewModel()) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(CATEGORY_FILTERS.first()) }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "Community Alerts", subtitle = "Alerts in Bwindi Impenetrable", onBack = onBack)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                items(CATEGORY_FILTERS) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            val filtered = selectedFilter.category?.let { category -> alerts.filter { it.category == category } } ?: alerts

            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.id }) { alert -> AlertCard(alert) }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    val (severityColor, iconColor, iconBg) = when (alert.severity) {
        AlertSeverity.URGENT -> Triple(Destructive, Destructive, Destructive.copy(alpha = 0.12f))
        AlertSeverity.CAUTION -> Triple(SunsetAmber, SunsetAmber, SunsetAmber.copy(alpha = 0.15f))
        AlertSeverity.INFO -> Triple(Info, Info, Info.copy(alpha = 0.15f))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconBadge(icon = categoryIcon(alert.category), background = iconBg, tint = iconColor)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(text = alert.severity.displayName(), contentColor = severityColor)
                }
                Text(
                    alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "${alert.location}  ·  ${relativeTime(alert.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun categoryIcon(category: AlertCategory): ImageVector = when (category) {
    AlertCategory.WILDLIFE -> Icons.Filled.WarningAmber
    AlertCategory.SAFETY -> Icons.Filled.Campaign
    AlertCategory.PATROLS -> Icons.Filled.NotificationsActive
    AlertCategory.TRAPPING -> Icons.Filled.WarningAmber
}

private fun AlertSeverity.displayName(): String = when (this) {
    AlertSeverity.URGENT -> "Urgent"
    AlertSeverity.CAUTION -> "Caution"
    AlertSeverity.INFO -> "Info"
}

private fun relativeTime(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - createdAt
    return when {
        diffMs < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)} min ago"
        diffMs < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)} hr ago"
        diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> {
            val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}
