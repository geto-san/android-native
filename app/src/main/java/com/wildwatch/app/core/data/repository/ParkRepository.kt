package com.wildwatch.app.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
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
}

@Singleton
class ParkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ParkRepository {

    override fun getParks(): Flow<List<NationalPark>> = flow {
        // Source.DEFAULT will first check the local cache then the server.
        val snapshot = firestore.collection("parks")
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(NationalPark::class.java))
    }

    override fun getAttractions(parkId: String): Flow<List<ParkAttraction>> = flow {
        val snapshot = firestore.collection("attractions")
            .whereEqualTo("parkId", parkId)
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(ParkAttraction::class.java))
    }

    override suspend fun findNearestPark(latitude: Double, longitude: Double): NationalPark? {
        val parks = firestore.collection("parks")
            .get(Source.DEFAULT)
            .await()
            .toObjects(NationalPark::class.java)
        
        return parks.minByOrNull { park ->
            val distLat = park.center.latitude() - latitude
            val distLng = park.center.longitude() - longitude
            distLat * distLat + distLng * distLng
        }
    }
}
