package com.silverback.sentry.data.observation

import android.net.Uri
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val OBSERVATIONS_COLLECTION = "observations"

@Singleton
class ObservationRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : ObservationRemoteDataSource {

    override suspend fun uploadImages(localUris: List<String>, observationId: String): List<String> {
        if (localUris.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()
        for ((index, uriString) in localUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${index}_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child("$OBSERVATIONS_COLLECTION/$observationId/$fileName")
                ref.putFile(uri).await()
                urls += ref.downloadUrl.await().toString()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                // Deliberately broad: one bad photo (unreadable file, transient
                // network error, ...) must not fail the whole upload batch - skip
                // it and keep going, matching the fixed RN behavior.
                Timber.e(e, "Failed to upload image %d for observation %s", index, observationId)
            }
        }
        return urls
    }

    override suspend fun writeDocument(id: String, data: Map<String, Any?>) {
        firestore.collection(OBSERVATIONS_COLLECTION).document(id).set(data).await()
    }

    override fun observeChanges(): Flow<List<RemoteObservationChange>> = callbackFlow {
        val query = firestore.collection(OBSERVATIONS_COLLECTION).orderBy("createdAt", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Observations snapshot listener error")
                return@addSnapshotListener
            }
            val changes = snapshot?.documentChanges?.map { change ->
                val id = change.document.id
                val data = change.document.data
                when (change.type) {
                    DocumentChange.Type.ADDED -> RemoteObservationChange.Added(id, data)
                    DocumentChange.Type.MODIFIED -> RemoteObservationChange.Modified(id, data)
                    DocumentChange.Type.REMOVED -> RemoteObservationChange.Removed(id)
                }
            } ?: return@addSnapshotListener
            trySend(changes)
        }
        awaitClose { registration.remove() }
    }
}
