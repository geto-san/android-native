package com.silverback.sentry

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SentryApplication : Application(), Configuration.Provider {

    // WorkManager's default androidx.startup initializer is disabled in the
    // manifest specifically so this custom, Hilt-aware configuration is the one
    // that takes effect - see the AndroidManifest.xml comment next to the
    // InitializationProvider override.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var observationRepository: ObservationRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        syncScheduler.schedulePeriodicSync()
        observationRepository.startObservingRemoteChanges()
    }
}
