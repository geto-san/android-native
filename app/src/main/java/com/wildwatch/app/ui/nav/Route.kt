package com.wildwatch.app.ui.nav

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes (Nav Compose 2.8+ @Serializable route objects)
// rather than manual string routes. `Main` is the authenticated app's entry point -
// a role-branching tab shell (see MainTabShell.kt: Dashboard/Feed/Profile for
// Community, Dashboard/Map/Tracking/Profile for Ranger) rendered as a single
// destination rather than one nav-graph route per tab, since none of the tabs
// need an independent back stack. Everything else pushes onto this outer NavHost
// from within whichever tab is showing.
sealed interface Route {
    @Serializable
    data object Auth : Route

    @Serializable
    data object Main : Route

    @Serializable
    data class IncidentDetail(val id: String) : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data class ReportIncident(val draftId: String? = null) : Route

    @Serializable
    data class ReportSubmitted(val incidentId: String) : Route

    @Serializable
    data class CameraCapture(val photoCategory: String? = null, val source: String = "sighting") : Route

    @Serializable
    data object CommunityAlerts : Route

    @Serializable
    data class ArticleDetail(val id: String) : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object IncidentHistory : Route
}
