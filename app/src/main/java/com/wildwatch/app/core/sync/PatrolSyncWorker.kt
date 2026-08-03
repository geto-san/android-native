package com.wildwatch.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wildwatch.app.core.data.patrol.PatrolRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PatrolSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val patrolRepository: PatrolRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = patrolRepository.syncPending()
        Timber.d("Patrol sync: %d succeeded, %d failed", syncResult.succeeded, syncResult.failed)

        return if (syncResult.failed > 0 && syncResult.succeeded == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
