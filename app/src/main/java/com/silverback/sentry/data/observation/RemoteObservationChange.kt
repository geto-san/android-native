package com.silverback.sentry.data.observation

// Mirrors Firestore's own DocumentChange.Type, kept as a separate sealed type
// here so ObservationRepositoryImpl's merge logic (guardrail G5) and its tests
// never need to touch a real Firestore DocumentChange.
sealed interface RemoteObservationChange {
    data class Added(val id: String, val data: Map<String, Any?>) : RemoteObservationChange
    data class Modified(val id: String, val data: Map<String, Any?>) : RemoteObservationChange
    data class Removed(val id: String) : RemoteObservationChange
}
