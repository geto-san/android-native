package com.silverback.sentry.domain.model

// Pure domain model - no FirebaseUser leaks past the data layer (guardrail G7:
// only data/ classes touch the Firebase SDK directly).
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
) {
    // Mirrors the RN app's fallback chain (ObservationContext.jsx's
    // `user?.displayName || user?.email?.split('@')[0] || 'Anonymous'`) so
    // display names stay consistent for the same accounts across both apps.
    val displayNameOrFallback: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: "Anonymous"
}
