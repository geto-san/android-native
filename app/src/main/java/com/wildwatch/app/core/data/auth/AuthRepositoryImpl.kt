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
import com.wildwatch.app.core.notifications.FcmTokenRepository
import com.wildwatch.app.core.notifications.FcmTopicManager
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

private fun com.google.firebase.auth.FirebaseUser.toDomain(role: UserRole, parkId: String?): User =
    User(
        uid = uid,
        email = email,
        displayName = displayName,
        role = role,
        parkId = parkId,
        isGuest = isAnonymous,
    )

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fcmTopicManager: FcmTopicManager,
    private val fcmTokenRepository: FcmTokenRepository,
    private val deviceSessionRepository: DeviceSessionRepository,
    private val deviceSessionStore: DeviceSessionStore,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    private val firebaseAuthFlow = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { auth ->
            launch {
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    fcmTopicManager.clearTopics()
                    deviceSessionRepository.clearLocalSession()
                    trySend(null)
                    return@launch
                }

                try {
                    val tokenResult = firebaseUser.getIdToken(false).await()
                    val roleStr = tokenResult.claims["role"] as? String
                    val parkId = tokenResult.claims["park_id"] as? String
                    val claimSessionVersion =
                        (tokenResult.claims["session_version"] as? Number)?.toInt() ?: 1
                    val localSessionVersion = deviceSessionStore.getSessionVersion()
                    if (localSessionVersion > 0 && claimSessionVersion != localSessionVersion) {
                        Timber.w(
                            "Session invalidated (local=%s token=%s) — signing out",
                            localSessionVersion,
                            claimSessionVersion,
                        )
                        firebaseAuth.signOut()
                        deviceSessionRepository.clearLocalSession()
                        trySend(null)
                        return@launch
                    }
                    val role = mapRole(roleStr)
                    fcmTopicManager.syncTopics(roleStr, parkId)
                    trySend(firebaseUser.toDomain(role, parkId))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get ID token for user role mapping")
                    trySend(firebaseUser.toDomain(UserRole.PUBLIC, null))
                }
            }
        }
        firebaseAuth.addIdTokenListener(listener)
        awaitClose { firebaseAuth.removeIdTokenListener(listener) }
    }

    override val currentUser: StateFlow<User?> = firebaseAuthFlow.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun signInAnonymously(): Result<Unit> = runCatching {
        firebaseAuth.signInAnonymously().await()
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdate = userProfileChangeRequest { this.displayName = displayName }
        result.user?.updateProfile(profileUpdate)?.await()
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun refreshRoleClaims(): Result<Unit> = runCatching {
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override fun signOut() {
        applicationScope.launch {
            fcmTopicManager.clearTopics()
            deviceSessionRepository.clearLocalSession()
        }
        firebaseAuth.signOut()
    }

    private suspend fun refreshClaimsAndTopics(forceRefresh: Boolean) {
        val firebaseUser = firebaseAuth.currentUser ?: return
        deviceSessionRepository.registerCurrentDevice()
        val tokenResult = firebaseUser.getIdToken(forceRefresh).await()
        val roleStr = tokenResult.claims["role"] as? String
        val parkId = tokenResult.claims["park_id"] as? String
        val claimSessionVersion =
            (tokenResult.claims["session_version"] as? Number)?.toInt() ?: 1
        deviceSessionStore.saveSessionVersion(claimSessionVersion)
        fcmTopicManager.syncTopics(roleStr, parkId)
        syncFcmRegistrationToken()
    }

    private fun mapRole(roleStr: String?): UserRole = when (roleStr?.lowercase()) {
        "ranger" -> UserRole.RANGER
        "warden" -> UserRole.WARDEN
        "uwa_official" -> UserRole.UWA_OFFICIAL
        else -> UserRole.PUBLIC
    }

    private suspend fun syncFcmRegistrationToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            fcmTokenRepository.syncToken(token)
        }.onFailure { Timber.w(it, "Failed to sync FCM registration token after auth") }
    }
}
