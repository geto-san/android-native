package com.wildwatch.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// A colored circular (or rounded-square) container around an icon - the
// "quick report tile" and "list row" icon treatment used throughout the
// wireframes (Community Alerts bell, recent-report species icon, claim
// category icon, etc).
@Composable
fun IconBadge(
    icon: ImageVector,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(
        modifier = modifier.size(size).background(background, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

// Small rounded-rect label used for statuses/severities (e.g. "Confirmed",
// "Urgent", "Escalated") - background is always a light tint of contentColor.
@Composable
fun StatusPill(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = contentColor.copy(alpha = 0.15f),
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

// Small circular numeric badge (e.g. the "3" unread-alerts count).
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
    contentColor: Color = MaterialTheme.colorScheme.error,
) {
    Box(
        modifier = modifier.size(28.dp).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}
