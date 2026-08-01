package com.wildwatch.app.core.data.auth

import android.os.Build
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSessionRepository @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val deviceSessionStore: DeviceSessionStore,
) {
    suspend fun registerCurrentDevice(): Result<Int> = runCatching {
        val deviceId = deviceSessionStore.getOrCreateDeviceId()
        val payload = mapOf(
            "deviceId" to deviceId,
            "deviceName" to Build.MODEL,
            "platform" to "android",
        )
        @Suppress("UNCHECKED_CAST")
        val response = firebaseFunctions
            .getHttpsCallable("syncActiveDevice")
            .call(payload)
            .await()
            .data as Map<String, Any?>

        val sessionVersion = (response["sessionVersion"] as? Number)?.toInt() ?: 1
        deviceSessionStore.saveSessionVersion(sessionVersion)
        if (response["evicted"] == true) {
            Timber.i("Evicted oldest device(s) to stay within active-device cap")
        }
        sessionVersion
    }.onFailure { Timber.w(it, "Failed to register active device session") }

    suspend fun clearLocalSession() {
        deviceSessionStore.clearSessionVersion()
    }
}
