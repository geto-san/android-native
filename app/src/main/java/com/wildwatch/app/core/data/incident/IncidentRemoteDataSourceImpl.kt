package com.wildwatch.app.core.data.incident

import android.net.Uri
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.wildwatch.app.core.model.Incident
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

    override suspend fun upsert(incident: Incident): Result<Unit> = runCatching {
        var finalIncident = incident
        if (incident.localImageUris.isNotEmpty()) {
            val urls = uploadImages(incident.localImageUris, incident.id)
            finalIncident = incident.copy(
                evidencePhotoUrls = incident.evidencePhotoUrls + urls,
                localImageUris = emptyList()
            )
        }
        firestore.collection(INCIDENTS_COLLECTION)
            .document(finalIncident.id)
            .set(finalIncident.toFirestoreMap())
            .await()
    }

    private suspend fun uploadImages(localUris: List<String>, incidentId: String): List<String> {
        val urls = mutableListOf<String>()
        for ((index, uriString) in localUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${index}_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child("$INCIDENTS_COLLECTION/$incidentId/$fileName")
                ref.putFile(uri).await()
                urls += ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload image %d for incident %s", index, incidentId)
            }
        }
        return urls
    }

    override fun observeChanges(): Flow<RemoteIncidentChange> = callbackFlow {
        val query = firestore.collection(INCIDENTS_COLLECTION).orderBy("reportedAt", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Incidents snapshot listener error")
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                val incident = Incident.fromFirestoreDocument(change.document.id, change.document.data)
                trySend(RemoteIncidentChange(
                    incident = incident,
                    isRemoved = change.type == DocumentChange.Type.REMOVED
                ))
            }
        }
        awaitClose { registration.remove() }
    }
}
