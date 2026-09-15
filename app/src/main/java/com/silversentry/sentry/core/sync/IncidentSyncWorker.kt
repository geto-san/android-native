package com.silversentry.sentry.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.incident.IncidentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class IncidentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
) : CoroutineWorker(appContext, workerParams) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        // Any lingering failure - a row that a previous pass marked FAILED, or a pass that was
        // itself interrupted by an unexpected exception - must cause a retry. The repository
        // already soldiers on per-row and isolates failures, so the only way syncPending itself
        // throws here is an outbox-read failure; retry that too rather than letting the work be
        // marked permanently failed (which used to leave everything still in the queue).
        return try {
            // A report/SOS raised before any sign-in sits in the outbox with no Firebase
            // identity behind it; Firestore rules reject those writes. The anonymous identity
            // the foreground save() path attempts may not have existed in time, so establish
            // it again here (best-effort - offline this fails fast and the next scheduled pass
            // retries).
            runCatching { authRepository.ensureSignedInForSubmission() }

            val syncResult = incidentRepository.syncPending()
            Timber.d(
                "Background sync: %d succeeded, %d failed",
                syncResult.succeeded,
                syncResult.failed,
            )

            if (syncResult.failed > 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Incident sync pass failed unexpectedly; will retry")
            Result.retry()
        }
    }
}
