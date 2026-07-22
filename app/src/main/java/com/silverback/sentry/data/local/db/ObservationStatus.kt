package com.silverback.sentry.data.local.db

// Domain status: has a ranger attended this sighting? Matches the Firestore
// 'status' field's two values ('pending'/'attended'). Not to be confused with
// SyncStatus, which tracks local<->remote sync state.
enum class ObservationStatus {
    PENDING,
    ATTENDED,
}
