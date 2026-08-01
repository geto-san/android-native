package com.wildwatch.app.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
private val KEY_SESSION_VERSION = intPreferencesKey("session_version")

@Singleton
class DeviceSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun getOrCreateDeviceId(): String {
        val prefs = dataStore.data.first()
        val existing = prefs[KEY_DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        dataStore.edit { it[KEY_DEVICE_ID] = created }
        return created
    }

    suspend fun getSessionVersion(): Int =
        dataStore.data.map { it[KEY_SESSION_VERSION] ?: 1 }.first()

    suspend fun saveSessionVersion(version: Int) {
        dataStore.edit { it[KEY_SESSION_VERSION] = version }
    }

    suspend fun clearSessionVersion() {
        dataStore.edit { it.remove(KEY_SESSION_VERSION) }
    }
}
