package com.wildwatch.app.feature.dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface CommunityAlertPreferences {
    val dismissedAlertIds: Flow<Set<String>>
    val seenAlertTimestamps: Flow<Map<String, Long>>
    suspend fun markSeen(alertId: String)
    suspend fun dismiss(alertId: String)
}

@Singleton
class CommunityAlertPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CommunityAlertPreferences {

    private val dismissedKey = stringSetPreferencesKey("dismissed_community_alerts")
    private fun seenKey(id: String) = longPreferencesKey("seen_alert_$id")

    override val dismissedAlertIds: Flow<Set<String>> = dataStore.data.map { it[dismissedKey] ?: emptySet() }

    override val seenAlertTimestamps: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            val name = key.name
            if (name.startsWith("seen_alert_") && value is Long) {
                name.removePrefix("seen_alert_") to value
            } else {
                null
            }
        }.toMap()
    }

    override suspend fun markSeen(alertId: String) {
        dataStore.edit { prefs -> prefs[seenKey(alertId)] = System.currentTimeMillis() }
    }

    override suspend fun dismiss(alertId: String) {
        dataStore.edit { prefs ->
            val current = prefs[dismissedKey] ?: emptySet()
            prefs[dismissedKey] = current + alertId
        }
    }
}
