package com.wildwatch.app.core.database

// Mirrors the wireframe's claim lifecycle. A claim always starts at
// UNDER_VERIFICATION on this device - every later transition (VERIFIED by a
// ranger, then APPROVED/PAID/REJECTED by UWA) only ever arrives as a remote
// Firestore change, never a local write, since this app has no ranger/UWA
// review UI.
enum class ClaimStatus {
    UNDER_VERIFICATION,
    VERIFIED,
    APPROVED,
    PAID,
    REJECTED,
}
