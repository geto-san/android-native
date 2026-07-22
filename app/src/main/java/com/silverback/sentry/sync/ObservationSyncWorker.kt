package com.silverback.sentry.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.silverback.sentry.data.observation.ObservationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

// Per guardrails G1/G3, doWork() is a one-line delegation to the repository's
// single atomic sync function - there is no separate Firestore-writing code
// here. This is deliberately the only thing that ever calls syncPending() from
// a background context, so there is exactly one sync engine, not a second
// parallel implementation living in a worker.
@HiltWorker
class ObservationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val observationRepository: ObservationRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = observationRepository.syncPending()
        Timber.d("Background sync: %d succeeded, %d failed", syncResult.succeeded, syncResult.failed)

        // Individual failed rows already self-heal (syncPending() reverts them to
        // PENDING for the next trigger, whatever that turns out to be) - only ask
        // WorkManager for its own backoff-and-retry when *nothing* went through,
        // which points at a systemic problem (e.g. connectivity flapped right
        // after the constraint was satisfied) rather than one bad observation.
        return if (syncResult.failed > 0 && syncResult.succeeded == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
