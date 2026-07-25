package com.wildwatch.app.data.incident

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

private const val INCIDENTS_COLLECTION = "incidents"

@Singleton
class IncidentRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : IncidentRemoteDataSource {

    override suspend fun uploadImages(localUris: List<String>, incidentId: String): List<String> {
        if (localUris.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()
        for ((index, uriString) in localUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${index}_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child("$INCIDENTS_COLLECTION/$incidentId/$fileName")
                ref.putFile(uri).await()
                urls += ref.downloadUrl.await().toString()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                // Deliberately broad: one bad photo (unreadable file, transient
                // network error, ...) must not fail the whole upload batch - skip
                // it and keep going, matching the fixed RN behavior.
                Timber.e(e, "Failed to upload image %d for incident %s", index, incidentId)
            }
        }
        return urls
    }

    override suspend fun writeDocument(id: String, data: Map<String, Any?>) {
        firestore.collection(INCIDENTS_COLLECTION).document(id).set(data).await()
    }

    override fun observeChanges(): Flow<List<RemoteIncidentChange>> = callbackFlow {
        val query = firestore.collection(INCIDENTS_COLLECTION).orderBy("reportedAt", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Incidents snapshot listener error")
                return@addSnapshotListener
            }
            val changes = snapshot?.documentChanges?.map { change ->
                val id = change.document.id
                val data = change.document.data
                when (change.type) {
                    DocumentChange.Type.ADDED -> RemoteIncidentChange.Added(id, data)
                    DocumentChange.Type.MODIFIED -> RemoteIncidentChange.Modified(id, data)
                    DocumentChange.Type.REMOVED -> RemoteIncidentChange.Removed(id)
                }
            } ?: return@addSnapshotListener
            trySend(changes)
        }
        awaitClose { registration.remove() }
    }
}
