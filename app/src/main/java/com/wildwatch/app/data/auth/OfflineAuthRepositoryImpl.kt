package com.wildwatch.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wildwatch.app.di.ApplicationScope
import com.wildwatch.app.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_UID = stringPreferencesKey("offline_auth_uid")
private val KEY_EMAIL = stringPreferencesKey("offline_auth_email")
private val KEY_PASSWORD_HASH = stringPreferencesKey("offline_auth_password_hash")
private val KEY_DISPLAY_NAME = stringPreferencesKey("offline_auth_display_name")
private val KEY_SESSION_ACTIVE = booleanPreferencesKey("offline_auth_session_active")

// TEMPORARY: stands in for AuthRepositoryImpl while this environment has no
// internet access to reach Firebase Auth. Persists a single local account +
// session flag to DataStore instead of calling FirebaseAuth, so sign up/sign
// in/sign out and the rest of the app (which only depends on the
// AuthRepository interface, never Firebase directly) keep working for
// on-device testing.
//
// TO REVERT once real connectivity/Firebase config is confirmed working:
// in di/RepositoryModule.kt, change
//   fun bindAuthRepository(impl: OfflineAuthRepositoryImpl): AuthRepository
// back to
//   fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
// This file and AuthRepositoryImpl.kt can both stay in the codebase either way.
@Singleton
class OfflineAuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = dataStore.data
        .map { prefs -> prefs.toUserIfSessionActive() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        val prefs = dataStore.data.first()
        val storedEmail = prefs[KEY_EMAIL]
        val storedHash = prefs[KEY_PASSWORD_HASH]
        if (storedEmail == null || storedHash == null) {
            return Result.failure(IllegalStateException("No offline account found on this device yet - register first."))
        }
        if (!storedEmail.equals(email.trim(), ignoreCase = true) || storedHash != hash(password)) {
            return Result.failure(IllegalArgumentException("Email or password doesn't match the offline account on this device."))
        }
        dataStore.edit { it[KEY_SESSION_ACTIVE] = true }
        return Result.success(Unit)
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> {
        dataStore.edit { prefs ->
            prefs[KEY_UID] = UUID.randomUUID().toString()
            prefs[KEY_EMAIL] = email.trim()
            prefs[KEY_PASSWORD_HASH] = hash(password)
            prefs[KEY_DISPLAY_NAME] = displayName.trim()
            prefs[KEY_SESSION_ACTIVE] = true
        }
        return Result.success(Unit)
    }

    override fun signOut() {
        // Fire-and-forget like FirebaseAuth.signOut() - the DataStore write
        // still lands via applicationScope even though this call isn't
        // suspending, matching the synchronous AuthRepository.signOut() signature.
        applicationScope.launch { dataStore.edit { it[KEY_SESSION_ACTIVE] = false } }
    }

    private fun Preferences.toUserIfSessionActive(): User? {
        if (this[KEY_SESSION_ACTIVE] != true) return null
        val uid = this[KEY_UID] ?: return null
        val email = this[KEY_EMAIL]
        val role = if (email?.lowercase() == "ranger@wildwatch.com") {
            com.wildwatch.app.domain.model.UserRole.RANGER
        } else {
            com.wildwatch.app.domain.model.UserRole.COMMUNITY
        }
        return User(uid = uid, email = email, displayName = this[KEY_DISPLAY_NAME], role = role)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
