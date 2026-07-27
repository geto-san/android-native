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
    // Placeholder for future user-specific preferences (e.g. biometric lock flag)
}

@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : UserDataRepository {
    // Implementation placeholder
}
