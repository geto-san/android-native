package com.wildwatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.wildwatch.app.ui.theme.ForestGreen
import com.wildwatch.app.ui.theme.ForestGreenGlow
import com.wildwatch.app.ui.theme.SunsetAmber

// The dark-green gradient panel with rounded bottom corners that anchors the
// top of the Welcome, auth, and Home screens.
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    bottomCornerRadius: androidx.compose.ui.unit.Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = bottomCornerRadius, bottomEnd = bottomCornerRadius))
            .background(Brush.verticalGradient(listOf(ForestGreen, ForestGreenGlow)))
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        content()
    }
}

// The rounded-square leaf mark used as the app's brand icon (splash, auth
// header, Welcome screen).
@Composable
fun WildWatchLogoMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(ForestGreenGlow.copy(alpha = 0.5f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Eco, contentDescription = null, tint = SunsetAmber, modifier = Modifier.size(size * 0.5f))
    }
}

// The circular initials avatar ("AN") shown on Home and wherever a compact
// user identity is needed.
@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
