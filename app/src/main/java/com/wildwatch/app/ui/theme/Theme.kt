package com.wildwatch.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Cream,
    primaryContainer = ForestGreenGlow,
    onPrimaryContainer = Cream,
    secondary = SavannaTan,
    onSecondary = Foreground,
    tertiary = SunsetAmber,
    onTertiary = Cream,
    background = Background,
    onBackground = Foreground,
    surface = Card,
    onSurface = Foreground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    error = Destructive,
    onError = Cream,
    outline = Border,
)

private val DarkColors = darkColorScheme(
    primary = ForestGreenGlow,
    onPrimary = Cream,
    primaryContainer = ForestGreen,
    onPrimaryContainer = Cream,
    secondary = SavannaTan,
    onSecondary = DarkBackground,
    tertiary = SunsetAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = Cream,
    surface = DarkSurface,
    onSurface = Cream,
    error = Destructive,
    onError = Cream,
)

// Every surface in the wireframes - cards, tiles, sheets, the report-form
// panels - uses a large, consistent corner radius rather than Material's
// default small rounding, so this is set globally instead of per-component.
private val WildWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Dynamic color (Material You) is available from Android 12+; we opt out of it by
// default so the app keeps its own WildWatch brand identity rather than following
// the device wallpaper palette.
@Composable
fun WildWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        shapes = WildWatchShapes,
        content = content,
    )
}
