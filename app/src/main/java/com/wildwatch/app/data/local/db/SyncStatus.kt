package com.wildwatch.app.data.local.db

// Local-only sync state machine (guardrail G3). Distinct from IncidentStatus,
// which is the domain lifecycle of the incident itself - these two are
// orthogonal and must never be conflated into one "status" column.
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}
