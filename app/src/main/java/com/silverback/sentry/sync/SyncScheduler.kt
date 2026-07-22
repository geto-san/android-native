package com.silverback.sentry.sync

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

private const val PERIODIC_SYNC_WORK_NAME = "observation_periodic_sync"
private const val IMMEDIATE_SYNC_WORK_NAME = "observation_immediate_sync"
private const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

    // Periodic backstop: WorkManager's own NetworkType.CONNECTED constraint means
    // this naturally waits for connectivity and fires on reconnect without any
    // separate connectivity listener - that's WorkManager's job, not this class's.
    // KEEP means calling this repeatedly (e.g. on every app launch) never
    // duplicates the periodic job (guardrail G3's "single sync engine" extends to
    // "single scheduled job", not just "single in-flight call").
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<ObservationSyncWorker>(
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

    // For "sync right now" moments (e.g. right after creating an observation
    // while online) rather than waiting for the next periodic cycle. REPLACE is
    // safe here: if one is already pending/running, the newer request only ever
    // finds the same set of PENDING rows the current run would have anyway.
    fun triggerImmediateSync() {
        val request = OneTimeWorkRequestBuilder<ObservationSyncWorker>()
            .setConstraints(networkConnectedConstraint())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun networkConnectedConstraint(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
