package com.wildwatch.app.core.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = InstaBlue,
    onPrimary = White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = Black,
    secondary = Grey500,
    onSecondary = White,
    tertiary = SunsetAmber,
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey500,
    error = Destructive,
    onError = White,
    outline = Grey300,
)

private val DarkColors = darkColorScheme(
    primary = InstaBlue,
    onPrimary = White,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = White,
    secondary = Grey500,
    onSecondary = Black,
    tertiary = SunsetAmber,
    onTertiary = Black,
    background = PureBlack,
    onBackground = White,
    surface = Color(0xFF121212),
    onSurface = White,
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Grey500,
    error = Destructive,
    onError = White,
    outline = Color(0xFF363636),
)

// Instagram-inspired shapes: cleaner, less aggressive rounding.
private val WildWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun WildWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Opt out for strict brand identity
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
