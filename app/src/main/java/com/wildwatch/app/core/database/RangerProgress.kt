package com.wildwatch.app.core.database

// The assigned ranger's operational sub-state, meaningful only while
// IncidentStatus == IN_PROGRESS and assignedTo is set. Null otherwise.
enum class RangerProgress {
    EN_ROUTE,
    ON_SITE,
}
