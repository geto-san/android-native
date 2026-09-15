package com.silversentry.sentry.core.database

// Authoritative lifecycle status, portal/dashboard-visible. Distinct from
// RangerProgress (a ranger's operational sub-state while IN_PROGRESS) and from
// isEscalated (an orthogonal urgency flag) - these three axes don't nest into
// one enum because the wireframe's own two status vocabularies (active/pending/
// resolved vs. en route/on site/resolved/escalated) don't nest either.
enum class IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,

    // An alert that was raised and then withdrawn by its reporter (e.g. a false-alarm SOS
    // cancelled via the long-press HOLD TO CANCEL on the live SOS screen). Written to
    // Firestore as "cancelled" so the withdrawal is honored everywhere - rangers' maps,
    // the community's realtime stream, and the web portal - instead of only locally.
    CANCELLED,
}
