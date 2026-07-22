package com.silverback.sentry.data.auth

import com.silverback.sentry.domain.model.User
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

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>

    fun signOut()
}
