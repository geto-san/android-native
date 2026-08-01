package com.wildwatch.app.core.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.wildwatch.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun syncToken(token: String) = withContext(ioDispatcher) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Timber.d("Skipping FCM token sync — no signed-in Firebase user")
            return@withContext
        }

        runCatching {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "fcm_tokens" to FieldValue.arrayUnion(token),
                    "fcm_token_updated_at" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
            Timber.d("Synced FCM token for user %s", uid)
        }.onFailure { Timber.w(it, "Failed to sync FCM token for user %s", uid) }
    }
}
