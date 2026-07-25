package com.wildwatch.app.data.local.db

// Authoritative lifecycle status, portal/dashboard-visible. Distinct from
// RangerProgress (a ranger's operational sub-state while IN_PROGRESS) and from
// isEscalated (an orthogonal urgency flag) - these three axes don't nest into
// one enum because the wireframe's own two status vocabularies (active/pending/
// resolved vs. en route/on site/resolved/escalated) don't nest either.
enum class IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
}
