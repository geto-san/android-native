package com.wildwatch.app.core.sync

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIODIC_SYNC_WORK_NAME = "incident_periodic_sync"
private const val IMMEDIATE_SYNC_WORK_NAME = "incident_immediate_sync"
private const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L

private const val PATROL_PERIODIC_SYNC_WORK_NAME = "patrol_periodic_sync"
private const val PATROL_IMMEDIATE_SYNC_WORK_NAME = "patrol_immediate_sync"
private const val PATROL_PERIODIC_SYNC_INTERVAL_MINUTES = 15L

private const val OFFLINE_MAP_DOWNLOAD_WORK_NAME_PREFIX = "offline_map_download_"

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

    // Periodic backstop: WorkManager's own NetworkType.CONNECTED constraint means
    // this naturally waits for connectivity and fires on reconnect without any
    // separate connectivity listener - that's WorkManager's job, not this class's.
    // KEEP means calling this repeatedly (e.g. on every app launch) never
    // duplicates the periodic job (guardrail G3's "single sync engine" extends to
    // "single scheduled job", not just "single in-flight call").
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<IncidentSyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(networkConnectedConstraint())
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    // For "sync right now" moments (e.g. right after creating an incident
    // while online) rather than waiting for the next periodic cycle. REPLACE is
    // safe here: if one is already pending/running, the newer request only ever
    // finds the same set of PENDING rows the current run would have anyway.
    fun triggerImmediateSync() {
        val request = OneTimeWorkRequestBuilder<IncidentSyncWorker>()
            .setConstraints(networkConnectedConstraint())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    // Same KEEP/REPLACE reasoning as the incident jobs above, mirrored for
    // patrol_logs - a separate uniquely-named job so the two sync engines
    // never contend with or cancel each other.
    fun schedulePeriodicPatrolSync() {
        val request = PeriodicWorkRequestBuilder<PatrolSyncWorker>(
            PATROL_PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(networkConnectedConstraint())
            .build()

        workManager.enqueueUniquePeriodicWork(
            PATROL_PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun triggerImmediatePatrolSync() {
        val request = OneTimeWorkRequestBuilder<PatrolSyncWorker>()
            .setConstraints(networkConnectedConstraint())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            PATROL_IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    // Triggered automatically (no per-download user consent prompt) once a ranger's assigned
    // park is confirmed - see OfflineMapCoordinator. KEEP + a per-park unique work name means
    // re-triggering on every login/app-restart is a no-op while the job is still
    // pending/running; if WorkManager has already pruned a finished record and this runs
    // again later, TileStore.loadTileRegion treats an already-cached region as a cheap
    // verification rather than a full re-download (see MapOfflineRepositoryImpl).
    fun scheduleOfflineMapDownload(parkId: String) {
        val request = OneTimeWorkRequestBuilder<MapOfflineDownloadWorker>()
            .setInputData(MapOfflineDownloadWorker.inputData(parkId))
            .setConstraints(unmeteredNetworkConstraint())
            .build()

        workManager.enqueueUniqueWork(
            OFFLINE_MAP_DOWNLOAD_WORK_NAME_PREFIX + parkId,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConnectedConstraint(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    // Unmetered (Wi-Fi) only - this download is silent/automatic, so it must never burn a
    // ranger's cellular data without them asking for it.
    private fun unmeteredNetworkConstraint(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
}
