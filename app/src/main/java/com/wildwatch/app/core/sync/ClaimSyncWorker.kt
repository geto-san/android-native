package com.wildwatch.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wildwatch.app.core.data.claim.ClaimRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ClaimSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val claimRepository: ClaimRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = claimRepository.syncPending()
        Timber.d("Background claim sync: %d succeeded", syncResult)

        return Result.success()
    }
}
