package com.wildwatch.app.core.data.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.wildwatch.app.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface UserDataRepository {
    val shouldShowWelcomeScreen: StateFlow<Boolean>
    fun dismissWelcomeScreen()
}

@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : UserDataRepository {

    private object PreferencesKeys {
        val SHOULD_SHOW_WELCOME = booleanPreferencesKey("should_show_welcome")
    }

    override val shouldShowWelcomeScreen: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOULD_SHOW_WELCOME] ?: true
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    override fun dismissWelcomeScreen() {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOULD_SHOW_WELCOME] = false
            }
        }
    }
}
