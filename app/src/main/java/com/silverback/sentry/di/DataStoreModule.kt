package com.silverback.sentry.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Single DataStore instance for the whole app. Per-feature preference repositories
// (e.g. Phase 10's UserPreferencesRepository for the per-uid biometric-lock flag,
// guardrail G6) namespace their own keys within it rather than each creating a
// separate DataStore file.
private const val PREFERENCES_DATASTORE_NAME = "silverback_sentry_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_DATASTORE_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
