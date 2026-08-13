package com.wildwatch.app.core.data.auth

import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.data.user.UserDataRepository
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val RANGER_SIGN_IN_POLICY_MESSAGE =
    "Ranger accounts must sign in with a Google account ending in @gmail.com."

/**
 * Ranger accounts are provisioned by a warden/gamepark officer inviting a real email address
 * (see the portal's "Invite personnel" flow) - anyone who later learns that address could
 * otherwise complete Firebase's passwordless email-link sign-in and claim the ranger role.
 * Business rule (not a technical necessity): a ranger session is only valid if it was
 * established via Google Sign-In (proves control of a real, already-signed-in Google account
 * on the device, not just an inbox) AND that account's address is specifically @gmail.com.
 * Pulled out as a standalone function (mirrors OfflineMapCoordinator's
 * shouldPrefetchOfflineMap) so the policy is unit-testable without a live FirebaseUser.
 */
internal fun violatesRangerSignInPolicy(providerIds: List<String>, email: String?): Boolean {
    val signedInWithGoogle = providerIds.contains(GoogleAuthProvider.PROVIDER_ID)
    val isGmailAddress = email?.endsWith("@gmail.com", ignoreCase = true) == true
    return !signedInWithGoogle || !isGmailAddress
}

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
    private val notificationRepository: NotificationRepository,
    private val userDataRepository: UserDataRepository,
    private val connectivityObserver: ConnectivityObserver,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    init {
        retryPendingAnonymousAuthWhenOnline()
    }

    // Backs AuthViewModel's offline-guest path: "Continue without account" tapped while
    // offline lets the user in locally right away (see AuthViewModel.signInAnonymously())
    // rather than blocking, and sets pendingAnonymousAuth so the real Firebase anonymous
    // identity still gets established transparently the next time the device is online -
    // without this, a guest who stays offline for their whole first session would never
    // actually get a real (syncable) anonymous account.
    private fun retryPendingAnonymousAuthWhenOnline() {
        applicationScope.launch {
            connectivityObserver.isOnline
                .filter { it }
                .collect {
                    if (firebaseAuth.currentUser == null && userDataRepository.pendingAnonymousAuth.first()) {
                        signInAnonymously().onSuccess {
                            userDataRepository.setPendingAnonymousAuth(false)
                        }
                    }
                }
        }
    }

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
                    if (role == UserRole.RANGER &&
                        violatesRangerSignInPolicy(firebaseUser.providerData.map { it.providerId }, firebaseUser.email)
                    ) {
                        Timber.w(
                            "Ranger session %s doesn't meet the Google/@gmail.com sign-in policy — signing out",
                            firebaseUser.uid,
                        )
                        firebaseAuth.signOut()
                        deviceSessionRepository.clearLocalSession()
                        trySend(null)
                        return@launch
                    }
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

    // The continue URL a clicked email link redirects through - see
    // android-native-backend-branch/hosting/ (Firebase Hosting, deployed to this exact
    // project) and this app's AndroidManifest App Link intent-filter for the same host.
    // Dynamic Links (the old way to do this without your own hosted URL) is sunset, so a
    // real hosted domain is required now, not optional.
    private val emailLinkActionCodeSettings: ActionCodeSettings by lazy {
        ActionCodeSettings.newBuilder()
            .setUrl("https://wildwatch-82abc.web.app/")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.wildwatch.app", true, null)
            .build()
    }

    override suspend fun sendSignInLinkToEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendSignInLinkToEmail(email, emailLinkActionCodeSettings).await()
    }

    override fun isSignInWithEmailLink(link: String): Boolean =
        firebaseAuth.isSignInWithEmailLink(link)

    override suspend fun signInWithEmailLink(email: String, link: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailLink(email, link).await()
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun refreshRoleClaims(): Result<Unit> = runCatching {
        refreshClaimsAndTopics(forceRefresh = true)
        Unit
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? =
        firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    override fun signOut() {
        applicationScope.launch {
            fcmTopicManager.clearTopics()
            deviceSessionRepository.clearLocalSession()
            notificationRepository.clearAll()
        }
        firebaseAuth.signOut()
    }

    private suspend fun refreshClaimsAndTopics(forceRefresh: Boolean) {
        val firebaseUser = firebaseAuth.currentUser ?: return
        deviceSessionRepository.registerCurrentDevice()
        val tokenResult = firebaseUser.getIdToken(forceRefresh).await()
        val roleStr = tokenResult.claims["role"] as? String

        // Checked right here (not just in the currentUser listener below) so a sign-in
        // attempt that violates the policy fails the actual signInWithGoogle()/
        // signInWithEmailLink() call with a clear error, instead of silently succeeding
        // and only getting bounced back to the sign-in screen a moment later once the
        // shared listener catches up.
        if (roleStr?.lowercase() == "ranger" &&
            violatesRangerSignInPolicy(firebaseUser.providerData.map { it.providerId }, firebaseUser.email)
        ) {
            firebaseAuth.signOut()
            deviceSessionRepository.clearLocalSession()
            throw IllegalStateException(RANGER_SIGN_IN_POLICY_MESSAGE)
        }

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
