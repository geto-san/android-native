package com.wildwatch.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.WildWatchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    // Using dummy data for the visual redesign demonstration
    val dummyNotifications = listOf(
        NotificationData("1", "John Ranger", "liked your wildlife sighting report.", "2h", Color(0xFF1B4332)),
        NotificationData("2", "Alice Warden", "confirmed your human-wildlife conflict report.", "5h", Color(0xFF2D6A4F)),
        NotificationData("3", "Park Bot", "New Community Alert: Elephant activity near Buhoma.", "1d", Color(0xFF409167)),
        NotificationData("4", "Tom Tourist", "is now following your conservation updates.", "2d", Color(0xFF52B788)),
        NotificationData("5", "Bob Official", "verified your profile as a community advocate.", "1w", Color(0xFF74C69D)),
        NotificationData("6", "WildWatch", "Welcome to Bwindi Impenetrable! Start your first patrol.", "2w", Color(0xFF95D5B2))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications", 
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(dummyNotifications, key = { it.id }) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

data class NotificationData(
    val id: String,
    val user: String,
    val action: String,
    val time: String,
    val avatarColor: Color
)

@Composable
private fun NotificationItem(notification: NotificationData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(notification.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification.user.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
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
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    WildWatchTheme {
        NotificationsScreen(onBack = {})
    }
}
