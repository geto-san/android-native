package com.silverback.sentry.ui.theme

import androidx.compose.ui.graphics.Color

// Wildwatch conservation palette (deep forest / savanna amber / sunset), converted
// 1:1 from the Loveable wireframe's oklch design tokens (src/styles.css) so the
// native app matches the approved UI designs pixel-for-pixel in color. This
// replaces the previous placeholder palette that matched the old Expo app.
val ForestPrimary = Color(0xFF185B37)
val ForestPrimaryGlow = Color(0xFF298646)
val ForestDeep = Color(0xFF0C3D22) // gradient-forest start
val ForestMid = Color(0xFF21763C) // gradient-forest end

val SavannaAccent = Color(0xFFEE921A)
val SunsetEnd = Color(0xFFF0503D) // gradient-sunset end

val SkyStart = Color(0xFF71BAD1) // gradient-sky start
val SkyEnd = Color(0xFFBAE0E2) // gradient-sky end

val StatusSuccess = Color(0xFF00A159)
val StatusWarning = Color(0xFFF2A618)
val StatusInfo = Color(0xFF008CBA)
val StatusDestructive = Color(0xFFDF2225)

val SurfaceBackground = Color(0xFFFCFAF4)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFEFF0E4)
val SurfaceSecondary = Color(0xFFF1EBD5)
val SurfaceBorder = Color(0xFFDEDFD4)

val ForegroundPrimary = Color(0xFF0B2010)
val ForegroundMuted = Color(0xFF5C6759)
val OnDark = Color(0xFFFCFAF3)

// Dark-theme equivalents (deepened, kept in the same green family rather than
// switching families, since Wildwatch's identity is the forest gradient itself).
val SurfaceBackgroundDark = Color(0xFF0F1A12)
val SurfaceCardDark = Color(0xFF16241A)