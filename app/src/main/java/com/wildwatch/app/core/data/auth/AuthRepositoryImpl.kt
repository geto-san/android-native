package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.wildwatch.app.core.di.ApplicationScope
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private fun com.google.firebase.auth.FirebaseUser.toDomain(role: UserRole): User =
    User(
        uid = uid,
        email = email,
        displayName = displayName,
        role = role,
        isGuest = isAnonymous
    )

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationScope applicationScope: CoroutineScope,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { auth ->
            launch {
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    trySend(null)
                    return@launch
                }

                try {
                    // Force refresh to get latest claims if needed
                    val tokenResult = firebaseUser.getIdToken(false).await()
                    val roleStr = tokenResult.claims["role"] as? String
                    val role = when (roleStr) {
                        "ranger" -> UserRole.RANGER
                        else -> UserRole.PUBLIC
                    }
                    trySend(firebaseUser.toDomain(role))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get ID token for user role mapping")
                    // Fallback to public if token fetch fails
                    trySend(firebaseUser.toDomain(UserRole.PUBLIC))
                }
            }
        }
        firebaseAuth.addIdTokenListener(listener)
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = null, // Initial value is unknown until listener fires
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun signInAnonymously(): Result<Unit> = runCatching {
        firebaseAuth.signInAnonymously().await()
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        Unit
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdate = userProfileChangeRequest { this.displayName = displayName }
        result.user?.updateProfile(profileUpdate)?.await()
        Unit
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
