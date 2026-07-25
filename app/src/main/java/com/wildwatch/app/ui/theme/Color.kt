package com.wildwatch.app.ui.theme

import androidx.compose.ui.graphics.Color

// WildWatch brand palette (converted from the wireframe's OKLCH design tokens
// in the WildWatch Community web prototype's styles.css - forest green /
// savanna tan / sunset amber, distinct from the old SilverBack Sentry look).
val ForestGreen = Color(0xFF185B37)
val ForestGreenGlow = Color(0xFF298646)
val Cream = Color(0xFFFCFAF3)
val SavannaTan = Color(0xFFF1EBD5)
val SunsetAmber = Color(0xFFEE921A)
val Destructive = Color(0xFFDF2225)
val Success = Color(0xFF00A159)
val Warning = Color(0xFFF2A618)
val Info = Color(0xFF008CBA)
val Background = Color(0xFFFCFAF4)
val Card = Color(0xFFFFFFFF)
val Foreground = Color(0xFF0B2010)
val Muted = Color(0xFFEFF0E4)
val MutedForeground = Color(0xFF5C6759)
val Border = Color(0xFFDEDFD4)

// No wireframe-specified dark palette - kept minimal, deriving a dark surface
// from the same brand hues rather than inventing an unrelated one.
val DarkBackground = Color(0xFF0B2010)
val DarkSurface = Color(0xFF14301E)
