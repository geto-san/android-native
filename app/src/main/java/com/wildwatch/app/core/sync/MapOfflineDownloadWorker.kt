package com.wildwatch.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.wildwatch.app.core.data.map.MapOfflineRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

private const val PARK_ID_KEY = "park_id"

@HiltWorker
class MapOfflineDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val parkRepository: ParkRepository,
    private val mapOfflineRepository: MapOfflineRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val parkId = inputData.getString(PARK_ID_KEY)
        if (parkId.isNullOrBlank()) {
            Timber.e("MapOfflineDownloadWorker started without a park_id input")
            return Result.failure()
        }

        val park = parkRepository.getPark(parkId)
        if (park == null) {
            Timber.e("Offline map download: park %s not found", parkId)
            return Result.failure()
        }

        return mapOfflineRepository.downloadParkRegion(park).fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                Timber.w(e, "Offline map download failed for park %s, will retry", parkId)
                Result.retry()
            },
        )
    }

    companion object {
        fun inputData(parkId: String): Data = Data.Builder().putString(PARK_ID_KEY, parkId).build()
    }
}
