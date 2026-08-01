package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.wildwatch.app.core.di.ApplicationScope
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private fun com.google.firebase.auth.FirebaseUser.toDomain(role: UserRole, parkId: String?): User =
    User(
        uid = uid,
        email = email,
        displayName = displayName,
        role = role,
        isGuest = isAnonymous,
        parkId = parkId
    )

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseMessaging: FirebaseMessaging,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    private val localGuestUser = MutableStateFlow<User?>(null)

    private val firebaseAuthFlow = callbackFlow {
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
                    val parkId = tokenResult.claims["park_id"] as? String
                    val role = when (roleStr) {
                        "ranger" -> UserRole.RANGER
                        else -> UserRole.PUBLIC
                    }
                    val domainUser = firebaseUser.toDomain(role, parkId)
                    trySend(domainUser)
                    
                    // Sync FCM token and topics whenever we get a valid user
                    syncFcmTokenAndTopics(domainUser)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get ID token for user role mapping")
                    // Fallback to public if token fetch fails
                    trySend(firebaseUser.toDomain(UserRole.PUBLIC, null))
                }
            }
        }
        firebaseAuth.addIdTokenListener(listener)
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }

    override val currentUser: StateFlow<User?> = combine(
        firebaseAuthFlow,
        localGuestUser
    ) { firebaseUser, guestUser ->
        firebaseUser ?: guestUser
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        localGuestUser.value = null
        Unit
    }

    override suspend fun signInAnonymously(): Result<Unit> = runCatching {
        firebaseAuth.signInAnonymously().await()
        localGuestUser.value = null
        Unit
    }

    override suspend fun continueAsGuest(): Result<Unit> = runCatching {
        localGuestUser.value = User(
            uid = "offline_guest_${java.util.UUID.randomUUID()}",
            email = null,
            displayName = "Guest User",
            role = UserRole.PUBLIC,
            isGuest = true
        )
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        localGuestUser.value = null
        Unit
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdate = userProfileChangeRequest { this.displayName = displayName }
        result.user?.updateProfile(profileUpdate)?.await()
        localGuestUser.value = null
        Unit
    }

    override fun signOut() {
        firebaseAuth.signOut()
        localGuestUser.value = null
    }

    private fun syncFcmTokenAndTopics(user: User) {
        applicationScope.launch {
            try {
                // 1. Sync FCM token to Firestore
                val token = firebaseMessaging.getToken().await()
                firestore.collection("users").document(user.uid)
                    .update("fcm_tokens", FieldValue.arrayUnion(token))
                    .await()

                // 2. Subscribe to topics
                firebaseMessaging.subscribeToTopic("park_alerts_all").await()
                
                val roleTopic = when (user.role) {
                    UserRole.RANGER -> "role_ranger"
                    UserRole.PUBLIC -> "role_public"
                }
                firebaseMessaging.subscribeToTopic(roleTopic).await()

                user.parkId?.let { parkId ->
                    firebaseMessaging.subscribeToTopic("park_alerts_$parkId").await()
                }

                Timber.d("FCM token and topics synced for user ${user.uid}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync FCM token or topics")
            }
        }
    }
}
