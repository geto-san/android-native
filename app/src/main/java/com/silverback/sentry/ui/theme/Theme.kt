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
    primary = SentryGreen,
    onPrimary = Color.White,
    secondary = SentryBlue,
    tertiary = SentryPurple,
    background = SurfaceLight,
    surface = Color.White,
    error = SentryRed,
)

private val DarkColors = darkColorScheme(
    primary = SentryGreen,
    onPrimary = Color.White,
    secondary = SentryBlue,
    tertiary = SentryPurple,
    background = SentryNavy,
    surface = SentryNavyLight,
    error = SentryRed,
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
