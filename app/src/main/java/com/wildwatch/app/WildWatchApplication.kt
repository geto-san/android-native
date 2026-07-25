package com.wildwatch.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wildwatch.app.data.claim.ClaimRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WildWatchApplication : Application(), Configuration.Provider {

    // WorkManager's default androidx.startup initializer is disabled in the
    // manifest specifically so this custom, Hilt-aware configuration is the one
    // that takes effect - see the AndroidManifest.xml comment next to the
    // InitializationProvider override.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var claimRepository: ClaimRepository

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
        incidentRepository.startObservingRemoteChanges()
        claimRepository.startObservingRemoteChanges()
    }
}
