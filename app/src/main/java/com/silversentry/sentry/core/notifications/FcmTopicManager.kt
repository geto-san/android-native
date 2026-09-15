package com.silversentry.sentry.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTopicManager @Inject constructor() {

    private var activeTopics: Set<String> = emptySet()

    suspend fun syncTopics(role: String?, parkId: String?) = withContext(Dispatchers.IO) {
        val desiredTopics = buildTopics(role, parkId)
        val toUnsubscribe = activeTopics - desiredTopics
        val toSubscribe = desiredTopics - activeTopics

        for (topic in toUnsubscribe) {
            runCatching {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
                Timber.d("Unsubscribed from FCM topic: $topic")
            }.onFailure { Timber.w(it, "Failed to unsubscribe from FCM topic: $topic") }
        }

        for (topic in toSubscribe) {
            runCatching {
                FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
                Timber.d("Subscribed to FCM topic: $topic")
            }.onFailure { Timber.w(it, "Failed to subscribe to FCM topic: $topic") }
        }

        activeTopics = desiredTopics
    }

    suspend fun clearTopics() = withContext(Dispatchers.IO) {
        for (topic in activeTopics) {
            runCatching {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            }.onFailure { Timber.w(it, "Failed to unsubscribe from FCM topic on sign-out: $topic") }
        }
        activeTopics = emptySet()
    }

    internal fun buildTopics(role: String?, parkId: String?): Set<String> {
        val normalizedRole = role?.trim()?.lowercase()
        val normalizedPark = parkId?.takeIf { it.isNotBlank() }?.let(::normalizeTopicSegment)

        val topics = linkedSetOf<String>()

        if (normalizedPark != null) {
            topics += "park_alerts_$normalizedPark"
        }

        // The community-feed fan-out (functions/src/notifications.ts sends every NEW_FEED_ARTICLE
        // push to park_alerts_all) must reach every signed-in device, ranger or public. This used
        // to be public-only, so a ranger's phone never got a feed notification - subscribed here
        // alongside the role-specific topics so both audiences stay in range.
        topics += "park_alerts_all"

        when (normalizedRole) {
            "ranger" -> if (normalizedPark != null) topics += "ranger_$normalizedPark"
            "warden" -> if (normalizedPark != null) topics += "warden_$normalizedPark"
            "uwa_official" -> topics += "uwa_official"
        }

        return topics
    }

    private fun normalizeTopicSegment(value: String): String =
        value.trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[\\s-]+"), "_")
            .lowercase()
}
