package com.wildwatch.app.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.wildwatch.app.core.model.District
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface LocationHierarchyRepository {
    fun getDistricts(): Flow<List<District>>
}

@Singleton
class LocationHierarchyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LocationHierarchyRepository {

    override fun getDistricts(): Flow<List<District>> = flow {
        val snapshot = firestore.collection("location_hierarchy")
            .get(Source.DEFAULT)
            .await()
        emit(snapshot.toObjects(District::class.java))
    }
}
