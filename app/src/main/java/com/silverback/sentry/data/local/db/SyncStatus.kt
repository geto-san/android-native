package com.silverback.sentry.data.local.db

// Local-only sync state machine (guardrail G3). Distinct from ObservationStatus,
// which is the domain concept of whether a ranger has attended the sighting -
// these two are orthogonal and must never be conflated into one "status" column.
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}
