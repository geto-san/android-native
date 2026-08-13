package com.wildwatch.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wildwatch.app.core.data.map.isMapboxTokenConfigured
import com.wildwatch.app.core.sync.OfflineMapCoordinator
import com.wildwatch.app.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WildWatchApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var offlineMapCoordinator: OfflineMapCoordinator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (isMapboxTokenConfigured()) {
            com.mapbox.common.MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        } else {
            Timber.e(
                "PUBLIC_MAPBOX_ACCESS_TOKEN is not set in local.properties - map screens will " +
                    "show an unavailable state instead of crashing.",
            )
        }

        // Initialize background sync cycles
        syncScheduler.schedulePeriodicSync()
        syncScheduler.schedulePeriodicPatrolSync()
        offlineMapCoordinator.start()
    }
}
