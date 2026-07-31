package com.wildwatch.app.core.database

// Local-only sync state machine (guardrail G3). Distinct from IncidentStatus,
// which is the domain lifecycle of the incident itself - these two are
// orthogonal and must never be conflated into one "status" column.
enum class SyncStatus {
    DRAFT,
    PENDING,
    PENDING_UPDATE,
    SYNCING,
    SYNCED,
    FAILED,
}
