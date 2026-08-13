package com.wildwatch.app.core.data.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wildwatch.app.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface UserDataRepository {
    val darkThemeConfig: Flow<Boolean?>
    suspend fun setDarkThemeConfig(dark: Boolean?)

    // The address a Firebase email sign-in link was last sent to, persisted between
    // AuthViewModel.sendEmailSignInLink() and the moment the user actually taps that link
    // (often minutes/hours later, in a fresh process) - Firebase's signInWithEmailLink()
    // needs the original email back, and it can't be recovered from the link URL alone.
    // Cleared once sign-in completes; if it's missing when a link arrives (a different
    // device, or storage was cleared), AuthViewModel falls back to asking the user to
    // confirm their email before completing sign-in.
    val pendingEmailLinkAddress: Flow<String?>
    suspend fun setPendingEmailLinkAddress(email: String?)
}

@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : UserDataRepository {

    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val pendingEmailLinkAddressKey = stringPreferencesKey("pending_email_link_address")

    override val darkThemeConfig: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[darkThemeKey]
    }

    override suspend fun setDarkThemeConfig(dark: Boolean?) {
        dataStore.edit { preferences ->
            if (dark == null) {
                preferences.remove(darkThemeKey)
            } else {
                preferences[darkThemeKey] = dark
            }
        }
    }

    override val pendingEmailLinkAddress: Flow<String?> = dataStore.data.map { preferences ->
        preferences[pendingEmailLinkAddressKey]
    }

    override suspend fun setPendingEmailLinkAddress(email: String?) {
        dataStore.edit { preferences ->
            if (email == null) {
                preferences.remove(pendingEmailLinkAddressKey)
            } else {
                preferences[pendingEmailLinkAddressKey] = email
            }
        }
    }
}
