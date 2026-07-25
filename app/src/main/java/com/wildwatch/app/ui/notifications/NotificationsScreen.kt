package com.wildwatch.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.data.local.db.NotificationType
import com.wildwatch.app.domain.model.Notification
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.IconBadge
import com.wildwatch.app.ui.theme.Destructive
import com.wildwatch.app.ui.theme.Info
import com.wildwatch.app.ui.theme.SunsetAmber
import com.wildwatch.app.ui.theme.Success
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// wireframe community.notifications - a personal activity feed reached via
// the bell icon on Home/Dashboard (the wireframe route itself has no linking
// entry point yet; a header bell is the idiomatic Android/Material spot for
// this per Now in Android and Material 3 guidance, and doubles for both
// Community and Ranger since MainTabShell wires it through for both roles).
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationViewModel = hiltViewModel()) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "Notifications", onBack = onBack)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(notification = notification, onClick = { viewModel.markAsRead(notification.id) })
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val (icon, tint) = notification.type.iconAndTint()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!notification.isRead) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                    } else {
                        Modifier
                    },
                )
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(icon = icon, background = tint.copy(alpha = 0.15f), tint = tint, size = 40.dp)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    relativeTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

private fun NotificationType.iconAndTint(): Pair<ImageVector, Color> = when (this) {
    NotificationType.SIGHTING_CONFIRMED -> Icons.Filled.CheckCircle to Success
    NotificationType.ALERT -> Icons.Filled.WarningAmber to Destructive
    NotificationType.CLAIM_UPDATE -> Icons.Filled.Receipt to SunsetAmber
    NotificationType.PATROL -> Icons.Filled.Campaign to Info
}

private fun relativeTime(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - createdAt
    return when {
        diffMs < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}m"
        diffMs < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}h"
        diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> {
            val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}
