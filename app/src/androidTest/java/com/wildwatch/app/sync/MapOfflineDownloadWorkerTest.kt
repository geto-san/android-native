package com.wildwatch.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.wildwatch.app.core.data.map.MapOfflineRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.sync.MapOfflineDownloadWorker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Verifies the worker's own glue logic (missing input, park lookup, Result
// translation) - not re-testing the download mechanics themselves, which
// live in MapOfflineRepositoryImpl and touch the real Mapbox native SDK
// (out of scope for either JVM or instrumented unit tests here).
@RunWith(AndroidJUnit4::class)
class MapOfflineDownloadWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun buildWorker(
        parkRepository: ParkRepository,
        mapOfflineRepository: MapOfflineRepository,
        inputData: androidx.work.Data = androidx.work.Data.EMPTY,
    ): MapOfflineDownloadWorker =
        TestListenableWorkerBuilder<MapOfflineDownloadWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = MapOfflineDownloadWorker(
                        appContext,
                        workerParameters,
                        parkRepository,
                        mapOfflineRepository,
                    )
                },
            )
            .build()

    @Test
    fun doWorkFailsWhenParkIdInputIsMissing() = runBlocking {
        val parkRepository = mockk<ParkRepository>()
        val mapOfflineRepository = mockk<MapOfflineRepository>()

        val result = buildWorker(parkRepository, mapOfflineRepository).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun doWorkFailsWhenParkIsNotFound() = runBlocking {
        val parkRepository = mockk<ParkRepository>()
        val mapOfflineRepository = mockk<MapOfflineRepository>()
        coEvery { parkRepository.getPark("park-1") } returns null

        val result = buildWorker(
            parkRepository,
            mapOfflineRepository,
            MapOfflineDownloadWorker.inputData("park-1"),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun doWorkSucceedsWhenDownloadSucceeds() = runBlocking {
        val parkRepository = mockk<ParkRepository>()
        val mapOfflineRepository = mockk<MapOfflineRepository>()
        val park = NationalPark(id = "park-1", name = "Bwindi")
        coEvery { parkRepository.getPark("park-1") } returns park
        coEvery { mapOfflineRepository.downloadParkRegion(park) } returns Result.success(Unit)

        val result = buildWorker(
            parkRepository,
            mapOfflineRepository,
            MapOfflineDownloadWorker.inputData("park-1"),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWorkRetriesWhenDownloadFails() = runBlocking {
        val parkRepository = mockk<ParkRepository>()
        val mapOfflineRepository = mockk<MapOfflineRepository>()
        val park = NationalPark(id = "park-1", name = "Bwindi")
        coEvery { parkRepository.getPark("park-1") } returns park
        coEvery { mapOfflineRepository.downloadParkRegion(park) } returns
            Result.failure(java.io.IOException("network down"))

        val result = buildWorker(
            parkRepository,
            mapOfflineRepository,
            MapOfflineDownloadWorker.inputData("park-1"),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }
}
