package com.wildwatch.app.core.data.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
}

@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : UserDataRepository {

    private val darkThemeKey = booleanPreferencesKey("dark_theme")

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
}
