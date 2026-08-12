package com.wildwatch.app.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing National Park metadata and attractions.
 * Uses Firestore with automatic local caching for offline support.
 */
interface ParkRepository {
    fun getParks(): Flow<List<NationalPark>>
    fun getAttractions(parkId: String): Flow<List<ParkAttraction>>
    suspend fun findNearestPark(latitude: Double, longitude: Double): NationalPark?
    suspend fun getPark(parkId: String): NationalPark?
    suspend fun createAttraction(attraction: ParkAttraction): Result<Unit>
}

@Singleton
class ParkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ParkRepository {

    // Every read below is wrapped against a real crash found by actually running the app:
    // an unguarded Firestore .await() throwing (e.g. PERMISSION_DENIED for an unauthenticated
    // guest, or plain offline-with-no-cache) propagated uncaught out of these coroutines and
    // took the whole app down rather than just failing this one screen's data load.

    override fun getParks(): Flow<List<NationalPark>> = flow {
        // Source.DEFAULT will first check the local cache then the server.
        val snapshot = firestore.collection("parks")
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(NationalPark::class.java))
    }.catch { error ->
        Timber.e(error, "Failed to load parks")
        emit(emptyList())
    }

    override fun getAttractions(parkId: String): Flow<List<ParkAttraction>> = flow {
        val snapshot = firestore.collection("pois")
            .whereEqualTo("parkId", parkId)
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(ParkAttraction::class.java))
    }.catch { error ->
        Timber.e(error, "Failed to load attractions for park %s", parkId)
        emit(emptyList())
    }

    override suspend fun findNearestPark(latitude: Double, longitude: Double): NationalPark? = try {
        val parks = firestore.collection("parks")
            .get(Source.DEFAULT)
            .await()
            .toObjects(NationalPark::class.java)

        parks.minByOrNull { park ->
            val distLat = park.center.latitude() - latitude
            val distLng = park.center.longitude() - longitude
            distLat * distLat + distLng * distLng
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to find nearest park")
        null
    }

    override suspend fun getPark(parkId: String): NationalPark? = try {
        firestore.collection("parks")
            .document(parkId)
            .get(Source.DEFAULT)
            .await()
            .toObject(NationalPark::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Failed to load park %s", parkId)
        null
    }

    override suspend fun createAttraction(attraction: ParkAttraction): Result<Unit> = runCatching {
        val doc = firestore.collection("pois").document()
        doc.set(attraction.copy(id = doc.id)).await()
    }
}
