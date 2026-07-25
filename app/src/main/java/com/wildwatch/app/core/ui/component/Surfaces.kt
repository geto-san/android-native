package com.wildwatch.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.wildwatch.app.core.ui.theme.DarkBackground
import com.wildwatch.app.core.ui.theme.ForestGreen
import com.wildwatch.app.core.ui.theme.ForestGreenGlow
import com.wildwatch.app.core.ui.theme.SunsetAmber

@Composable
fun ForestBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ForestGreen, DarkBackground)
                )
            )
    ) {
        content()
    }
}

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

@Composable
fun WildWatchLogoMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    color: androidx.compose.ui.graphics.Color = SunsetAmber
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(size)
    ) {
        val strokeWidth = size.toPx() * 0.12f

        // Large Chevron (The "Wild" / Mountain)
        val largePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.toPx() * 0.20f, size.toPx() * 0.70f)
            lineTo(size.toPx() * 0.45f, size.toPx() * 0.30f)
            lineTo(size.toPx() * 0.70f, size.toPx() * 0.70f)
        }

        // Small Chevron (The "Watch" / Precision)
        val smallPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.toPx() * 0.55f, size.toPx() * 0.70f)
            lineTo(size.toPx() * 0.75f, size.toPx() * 0.45f)
            lineTo(size.toPx() * 0.95f, size.toPx() * 0.70f)
        }

        drawPath(
            path = largePath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        drawPath(
            path = smallPath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

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
