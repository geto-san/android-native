package com.wildwatch.app.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.ui.theme.SunsetAmber

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
