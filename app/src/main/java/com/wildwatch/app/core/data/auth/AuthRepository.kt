package com.wildwatch.app.core.data.auth

import com.wildwatch.app.core.model.User
import kotlinx.coroutines.flow.StateFlow

// Per guardrail G7, this is the only auth surface the UI layer is allowed to depend
// on - AuthViewModel injects this interface, never FirebaseAuth directly.
interface AuthRepository {

    // Current signed-in user, or null if signed out. Backed by FirebaseAuth's own
    // AuthStateListener so it stays correct across process restarts without any
    // extra bookkeeping (mirrors what onAuthStateChanged did in the RN app's
    // AuthContext.jsx, just via a StateFlow instead of a React state hook).
    val currentUser: StateFlow<User?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Firebase anonymous auth — use for guest browse, reporting, and notifications. */
    suspend fun signInAnonymously(): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>

    fun signOut()

    /** Force-refresh Firebase custom claims and re-sync FCM topic subscriptions. */
    suspend fun refreshRoleClaims(): Result<Unit>

    /**
     * The current user's Firebase ID token, for authenticating calls the app makes directly to
     * the Laravel API (see LaravelBridgeDataSource) - null if signed out. Firebase caches the
     * token and refreshes it locally when it's close to expiring, so [forceRefresh] is normally
     * unnecessary; pass true only to force a fresh token immediately (e.g. after the server
     * reported it as invalid/expired).
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String?
}
