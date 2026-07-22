package com.silverback.sentry.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.silverback.sentry.di.ApplicationScope
import com.silverback.sentry.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private fun com.google.firebase.auth.FirebaseUser?.toDomain(): User? =
    this?.let { User(uid = it.uid, email = it.email, displayName = it.displayName) }

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationScope applicationScope: CoroutineScope,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = firebaseAuth.currentUser.toDomain(),
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
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
