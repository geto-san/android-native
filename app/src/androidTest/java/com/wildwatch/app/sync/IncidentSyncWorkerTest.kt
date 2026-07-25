package com.wildwatch.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.incident.SyncResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Verifies the worker delegates to IncidentRepository.syncPending() (the
// single sync engine) and translates its SyncResult into the right
// WorkManager Result - not re-testing sync logic itself, which already has
// its own heavy coverage in IncidentRepositoryImplTest.
@RunWith(AndroidJUnit4::class)
class IncidentSyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun buildWorker(repository: IncidentRepository): IncidentSyncWorker {
        val worker = TestListenableWorkerBuilder<IncidentSyncWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = IncidentSyncWorker(appContext, workerParameters, repository)
                },
            )
            .build()
        return worker
    }

    @Test
    fun doWorkReturnsSuccessWhenEverythingSyncs() = runBlocking {
        val repository = mockk<IncidentRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 2, failed = 0)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkReturnsSuccessWhenNothingWasPending() = runBlocking {
        val repository = mockk<IncidentRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 0, failed = 0)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkReturnsSuccessWhenSomeRowsSucceedAndSomeFail() = runBlocking {
        val repository = mockk<IncidentRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 1, failed = 1)

        val result = buildWorker(repository).doWork()

        // Partial success: failed rows already reverted to PENDING and will be
        // retried by the next trigger - no need for WorkManager's own backoff too.
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkRequestsRetryWhenNothingSucceeds() = runBlocking {
        val repository = mockk<IncidentRepository>()
        coEvery { repository.syncPending() } returns SyncResult(succeeded = 0, failed = 3)

        val result = buildWorker(repository).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }
}
