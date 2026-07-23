package com.silverback.sentry.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.silverback.sentry.ui.theme.StatusDestructive
import com.silverback.sentry.ui.theme.StatusInfo
import com.silverback.sentry.ui.theme.StatusSuccess
import com.silverback.sentry.ui.theme.StatusWarning
import com.silverback.sentry.ui.theme.SurfaceMuted

/** Mirrors the wireframe's `tone` prop (default/success/warning/danger/info). */
enum class PillTone { Default, Success, Warning, Danger, Info }

/**
 * Direct port of `Pill` from ui-prototype.tsx — the small rounded status/count
 * badge used on report rows, alert counts, and claim statuses throughout the app.
 */
@Composable
fun Pill(text: String, tone: PillTone = PillTone.Default, modifier: Modifier = Modifier) {
    val (background, foreground) = when (tone) {
        PillTone.Default -> SurfaceMuted to MaterialTheme.colorScheme.onSurfaceVariant
        PillTone.Success -> StatusSuccess.copy(alpha = 0.15f) to StatusSuccess
        PillTone.Warning -> StatusWarning.copy(alpha = 0.20f) to MaterialTheme.colorScheme.onBackground
        PillTone.Danger -> StatusDestructive.copy(alpha = 0.15f) to StatusDestructive
        PillTone.Info -> StatusInfo.copy(alpha = 0.15f) to StatusInfo
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}