package com.wildwatch.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wildwatch.app.data.claim.ClaimRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

// Mirrors IncidentSyncWorker - see its own doc comment.
@HiltWorker
class ClaimSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val claimRepository: ClaimRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = claimRepository.syncPending()
        Timber.d("Background claim sync: %d succeeded, %d failed", syncResult.succeeded, syncResult.failed)

        return if (syncResult.failed > 0 && syncResult.succeeded == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
