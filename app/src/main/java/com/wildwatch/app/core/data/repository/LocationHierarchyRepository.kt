package com.wildwatch.app.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.wildwatch.app.core.model.District
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface LocationHierarchyRepository {
    fun getDistricts(): Flow<List<District>>
}

@Singleton
class LocationHierarchyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LocationHierarchyRepository {

    // See ParkRepository for why this is guarded - an unguarded Firestore .await() throwing
    // here crashed the whole app, not just this one read, when it was found on-device.
    override fun getDistricts(): Flow<List<District>> = flow {
        val snapshot = firestore.collection("location_hierarchy")
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(District::class.java))
    }.catch { error ->
        Timber.e(error, "Failed to load location hierarchy")
        emit(emptyList())
    }
}
