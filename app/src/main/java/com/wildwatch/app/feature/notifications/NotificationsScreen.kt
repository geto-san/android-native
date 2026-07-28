package com.wildwatch.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildwatch.app.core.database.NotificationType
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.WildWatchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToIncident: (String) -> Unit = {},
    onNavigateToArticle: (String) -> Unit = {},
    onNavigateToAlerts: () -> Unit = {}
) {
    val dummyNotifications = listOf(
        NotificationUiData("1", "John Ranger", "liked your wildlife sighting report.", "2h", Color(0xFF1B4332), NotificationType.LIKE, "incident_1"),
        NotificationUiData("2", "Alice Warden", "commented: \"Great capture! We'll investigate.\"", "5h", Color(0xFF2D6A4F), NotificationType.COMMENT, "incident_2"),
        NotificationUiData("3", "Park Bot", "New Community Alert: Elephant activity near Buhoma.", "1d", Color(0xFF409167), NotificationType.SECURITY_ALERT),
        NotificationUiData("4", "Tom Tourist", "liked your human-wildlife conflict report.", "2d", Color(0xFF52B788), NotificationType.LIKE, "incident_3"),
        NotificationUiData("5", "WildWatch News", "published: \"Protecting Bwindi: A New Decade.\"", "1w", Color(0xFF74C69D), NotificationType.NEW_FEED_ARTICLE, "article_1"),
        NotificationUiData("6", "WildWatch", "Welcome to Bwindi Impenetrable! Your first report is live.", "2w", Color(0xFF95D5B2), NotificationType.SYSTEM)
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            dummyNotifications.forEach { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = {
                        when (notification.type) {
                            NotificationType.LIKE, 
                            NotificationType.COMMENT -> notification.targetId?.let { onNavigateToIncident(it) }
                            NotificationType.NEW_FEED_ARTICLE -> notification.targetId?.let { onNavigateToArticle(it) }
                            NotificationType.SECURITY_ALERT -> onNavigateToAlerts()
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

data class NotificationUiData(
    val id: String,
    val user: String,
    val action: String,
    val time: String,
    val avatarColor: Color,
    val type: NotificationType,
    val targetId: String? = null
)

@Composable
private fun NotificationItem(
    notification: NotificationUiData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(notification.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification.user.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(notification.user)
                    }
                    append(" ")
                    append(notification.action)
                    append(" ")
                    withStyle(style = SpanStyle(color = Grey500, fontSize = 13.sp)) {
                        append(notification.time)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp
            )
        }

        if (notification.targetId != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
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
