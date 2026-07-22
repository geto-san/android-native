package com.silverback.sentry.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.data.observation.SyncResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Verifies the worker delegates to ObservationRepository.syncPending() (the
// single sync engine from Phase 4) and translates its SyncResult into the
// right WorkManager Result - not re-testing sync logic itself, which already
// has its own heavy coverage in ObservationRepositoryImplTest.
@RunWith(AndroidJUnit4::class)
class ObservationSyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun buildWorker(repository: ObservationRepository): ObservationSyncWorker {
        val worker = TestListenableWorkerBuilder<ObservationSyncWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = ObservationSyncWorker(appContext, workerParameters, repository)
                },
            )
            .build()
        return worker
    }

    @Test
    fun doWorkReturnsSuccessWhenEverythingSyncs() = runBlocking {
        val repository = mockk<ObservationRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 2, failed = 0)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkReturnsSuccessWhenNothingWasPending() = runBlocking {
        val repository = mockk<ObservationRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 0, failed = 0)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkReturnsSuccessWhenSomeRowsSucceedAndSomeFail() = runBlocking {
        val repository = mockk<ObservationRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 1, failed = 1)

        val result = buildWorker(repository).doWork()

        // Partial success: failed rows already reverted to PENDING and will be
        // retried by the next trigger - no need for WorkManager's own backoff too.
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkRequestsRetryWhenNothingSucceeds() = runBlocking {
        val repository = mockk<ObservationRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 0, failed = 3)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }
}
