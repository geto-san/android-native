package com.wildwatch.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wildwatch.app.core.data.incident.IncidentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class IncidentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = incidentRepository.syncPending()
        Timber.d("Background sync: %d succeeded, %d failed", syncResult.succeeded, syncResult.failed)

        return if (syncResult.failed > 0 && syncResult.succeeded == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
