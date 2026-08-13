package com.wildwatch.app.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.ui.theme.SunsetAmber

// Mirrors app/src/main/res/drawable/ic_launcher_foreground.xml (the ring + interlocking
// double-V "W" mark from art/icons/Use this as the new icon for the app.webp) so the login
// screen's logo always matches the launcher icon instead of drifting into its own design.
@Composable
fun WildWatchLogoMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    color: androidx.compose.ui.graphics.Color = SunsetAmber
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(size)
    ) {
        val px = size.toPx()
        val ringStroke = px * 0.046f
        val vStroke = px * 0.056f
        val vStyle = androidx.compose.ui.graphics.drawscope.Stroke(
            width = vStroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )

        drawCircle(
            color = color,
            radius = px * 0.25f,
            center = androidx.compose.ui.geometry.Offset(px * 0.5f, px * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = ringStroke)
        )

        val leftV = androidx.compose.ui.graphics.Path().apply {
            moveTo(px * 0.333f, px * 0.361f)
            lineTo(px * 0.463f, px * 0.611f)
            lineTo(px * 0.593f, px * 0.361f)
        }
        val rightV = androidx.compose.ui.graphics.Path().apply {
            moveTo(px * 0.435f, px * 0.361f)
            lineTo(px * 0.565f, px * 0.611f)
            lineTo(px * 0.694f, px * 0.361f)
        }

        drawPath(path = leftV, color = color, style = vStyle)
        drawPath(path = rightV, color = color, style = vStyle)
    }
}
