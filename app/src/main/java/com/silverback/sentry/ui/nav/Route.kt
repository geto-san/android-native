package com.silverback.sentry.ui.nav

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes (Nav Compose 2.8+ @Serializable route objects)
// rather than manual string routes. `Main` (the Home dashboard) is the
// authenticated app's entry point - a full tab shell (Map/Chat/Assistant/
// Diagnostics/Settings) arrives in later phases.
sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object Main : Route

    @Serializable
    data object Feed : Route

    @Serializable
    data object AddSighting : Route

    @Serializable
    data object CameraCapture : Route
}
