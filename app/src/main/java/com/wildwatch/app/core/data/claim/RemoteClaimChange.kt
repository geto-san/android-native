package com.wildwatch.app.data.claim

// Mirrors RemoteIncidentChange - see its own doc comment.
sealed interface RemoteClaimChange {
    data class Added(val id: String, val data: Map<String, Any?>) : RemoteClaimChange
    data class Modified(val id: String, val data: Map<String, Any?>) : RemoteClaimChange
    data class Removed(val id: String) : RemoteClaimChange
}
