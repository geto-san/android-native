package com.wildwatch.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.model.Notification
import com.wildwatch.app.core.ui.component.IconBadge
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.WildWatchTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToIncident: (String) -> Unit = {},
    onNavigateToArticle: (String) -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Activity",
                        style = MaterialTheme.typography.titleLarge,
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
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nothing here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Grey500,
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(notifications, key = { it.id }) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = {
                        viewModel.markRead(notification.id)
                        when (notification.type) {
                            NotificationType.SECURITY_ALERT -> onNavigateToAlerts()
                            NotificationType.SIGHTING_APPROVED ->
                                notification.targetId?.let(onNavigateToIncident)
                            NotificationType.NEW_FEED_ARTICLE ->
                                notification.targetId?.let(onNavigateToArticle)
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.SECURITY_ALERT -> Icons.Filled.Warning
    NotificationType.SIGHTING_APPROVED -> Icons.Filled.CheckCircle
    NotificationType.NEW_FEED_ARTICLE -> Icons.AutoMirrored.Filled.Article
    NotificationType.PENDING_SYNC -> Icons.Filled.CloudUpload
    NotificationType.SYSTEM, NotificationType.LIKE, NotificationType.COMMENT -> Icons.Filled.Info
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = iconFor(notification.type),
            background = MaterialTheme.colorScheme.surfaceVariant,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
            )
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = Grey500,
                maxLines = 2,
            )
            Text(
                text = relativeTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = Grey500,
            )
        }

        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
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

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    WildWatchTheme {
        NotificationsScreen(onBack = {})
    }
}
