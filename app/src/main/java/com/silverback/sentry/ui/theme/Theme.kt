package com.silverback.sentry.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = OnDark,
    primaryContainer = SurfaceSecondary,
    onPrimaryContainer = ForegroundPrimary,
    secondary = SavannaAccent,
    onSecondary = ForegroundPrimary,
    tertiary = StatusInfo,
    background = SurfaceBackground,
    onBackground = ForegroundPrimary,
    surface = SurfaceCard,
    onSurface = ForegroundPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = ForegroundMuted,
    outline = SurfaceBorder,
    error = StatusDestructive,
    onError = OnDark,
)

private val DarkColors = darkColorScheme(
    primary = ForestPrimaryGlow,
    onPrimary = ForegroundPrimary,
    secondary = SavannaAccent,
    onSecondary = ForegroundPrimary,
    tertiary = StatusInfo,
    background = SurfaceBackgroundDark,
    onBackground = OnDark,
    surface = SurfaceCardDark,
    onSurface = OnDark,
    error = StatusDestructive,
    onError = OnDark,
)

// Dynamic color (Material You) is available from Android 12+; we opt out of it by
// default so the app keeps its own brand identity rather than following the device
// wallpaper palette, matching the fixed brand look of the current Expo app.
@Composable
fun SilverBackSentryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}