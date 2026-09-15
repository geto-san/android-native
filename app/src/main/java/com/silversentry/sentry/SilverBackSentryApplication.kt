package com.silversentry.sentry

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.silversentry.sentry.core.data.map.isMapboxTokenConfigured
import com.silversentry.sentry.core.notifications.Notifications
import com.silversentry.sentry.core.sync.OfflineMapCoordinator
import com.silversentry.sentry.core.sync.OutboxSyncListener
import com.silversentry.sentry.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SilverBackSentryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var offlineMapCoordinator: OfflineMapCoordinator

    @Inject
    lateinit var outboxSyncListener: OutboxSyncListener

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // All notification channels (alerts, patrol tracking) are registered in one
        // place up-front, mihon-style, rather than lazily at first post time.
        Notifications.createChannels(this)

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
        outboxSyncListener.start()
        offlineMapCoordinator.start()
    }
}
