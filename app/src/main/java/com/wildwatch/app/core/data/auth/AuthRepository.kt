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

    /** Firebase anonymous auth — use for guest browse, reporting, and notifications. */
    suspend fun signInAnonymously(): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /**
     * Sends a Firebase email sign-in link to [email]. Passwordless by design (see
     * AuthViewModel/AuthScreen KDoc): a link only a real inbox owner can click is what makes
     * this a genuine email-ownership check, unlike a self-reported password - and it doubles
     * as sign-up, since Firebase creates the account automatically on first successful link
     * click. There is deliberately no separate password-based sign-in/sign-up path anymore.
     */
    suspend fun sendSignInLinkToEmail(email: String): Result<Unit>

    /** Local, synchronous check - true if [link] is a Firebase email sign-in link. */
    fun isSignInWithEmailLink(link: String): Boolean

    /** Completes the passwordless flow once the user has actually clicked the emailed link. */
    suspend fun signInWithEmailLink(email: String, link: String): Result<Unit>

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
