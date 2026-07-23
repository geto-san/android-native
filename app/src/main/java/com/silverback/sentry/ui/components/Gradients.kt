package com.silverback.sentry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import com.silverback.sentry.ui.theme.ForestDeep
import com.silverback.sentry.ui.theme.ForestMid
import com.silverback.sentry.ui.theme.SavannaAccent
import com.silverback.sentry.ui.theme.SkyEnd
import com.silverback.sentry.ui.theme.SkyStart
import com.silverback.sentry.ui.theme.SunsetEnd

/**
 * Direct ports of the wireframe's `gradient-forest` / `gradient-sunset` /
 * `gradient-sky` CSS utilities (src/styles.css, 135deg / 180deg linear-gradients).
 * Used on header bands, splash backgrounds, and map placeholders across every
 * Wildwatch screen, so this is shared once here rather than redefined per screen.
 */
object WildwatchGradients {
    @Composable
    fun forest(): Brush = Brush.linearGradient(listOf(ForestDeep, ForestMid))

    @Composable
    fun sunset(): Brush = Brush.linearGradient(listOf(SavannaAccent, SunsetEnd))

    @Composable
    fun sky(): Brush = Brush.verticalGradient(listOf(SkyStart, SkyEnd))
}