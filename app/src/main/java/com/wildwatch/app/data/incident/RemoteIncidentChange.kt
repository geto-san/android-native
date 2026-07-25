package com.wildwatch.app.data.incident

// Mirrors Firestore's own DocumentChange.Type, kept as a separate sealed type
// here so IncidentRepositoryImpl's merge logic (guardrail G5) and its tests
// never need to touch a real Firestore DocumentChange.
sealed interface RemoteIncidentChange {
    data class Added(val id: String, val data: Map<String, Any?>) : RemoteIncidentChange
    data class Modified(val id: String, val data: Map<String, Any?>) : RemoteIncidentChange
    data class Removed(val id: String) : RemoteIncidentChange
}
