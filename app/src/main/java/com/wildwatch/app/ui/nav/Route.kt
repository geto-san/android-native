package com.wildwatch.app.ui.nav

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes (Nav Compose 2.8+ @Serializable route objects)
// rather than manual string routes. `Main` is the authenticated app's entry point -
// a 3-tab shell (Home/Feed/SOS, see MainTabShell.kt) rendered as a single
// destination rather than three separate nav-graph routes, since none of the tabs
// need an independent back stack. Everything else pushes onto this outer NavHost
// from within whichever tab is showing.
//
// Dashboard/Tracking (the pre-redesign ranger-facing screens) are kept in the
// codebase but intentionally have no route here anymore - the wireframes this app
// now matches don't include them, so they're unreferenced until a future task
// decides how (or whether) they rejoin the citizen-facing workflow. The old
// ranger-styled MapScreen was retired outright (not just unlinked) once
// CommunityMapScreen replaced it with the same MapViewModel/MarkerColor data
// layer behind the new design system.
sealed interface Route {
    @Serializable
    data object Welcome : Route

    @Serializable
    data class Auth(val startOnSignIn: Boolean) : Route

    @Serializable
    data object Main : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Tracking : Route

    @Serializable
    data class IncidentDetail(val id: String) : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object WildlifeSightingReport : Route

    @Serializable
    data object ConflictReport : Route

    @Serializable
    data class ReportSubmitted(val incidentId: String) : Route

    @Serializable
    data class CameraCapture(val photoCategory: String? = null, val source: String = "sighting") : Route

    @Serializable
    data object CommunityAlerts : Route

    @Serializable
    data object CompensationClaim : Route

    @Serializable
    data class NewClaim(val category: String) : Route

    @Serializable
    data object CommunityMap : Route
}
