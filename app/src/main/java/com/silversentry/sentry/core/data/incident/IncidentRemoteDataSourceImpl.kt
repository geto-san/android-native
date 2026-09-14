package com.silversentry.sentry.core.data.incident

import android.net.Uri
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.model.Incident
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val INCIDENTS_COLLECTION = "incidents"
private const val SOS_ALERTS_COLLECTION = "sos_alerts"
private const val SIGHTINGS_COLLECTION = "sightings"

// The three Firestore collections a locally-typed Incident can live in server-side. SOS and
// SIGHTING each have their own purpose-built collection (and Laravel table -
// SosAlert/WildlifeSighting) with its own security rules, portal dashboard, and - now that
// this branches by type instead of always writing to "incidents" - its own live realtime
// stream. Everything else still shares the generic "incidents" collection.
private fun collectionFor(type: IncidentType): String = when (type) {
    IncidentType.SOS -> SOS_ALERTS_COLLECTION
    IncidentType.SIGHTING -> SIGHTINGS_COLLECTION
    else -> INCIDENTS_COLLECTION
}

private val ALL_COLLECTIONS = listOf(INCIDENTS_COLLECTION, SOS_ALERTS_COLLECTION, SIGHTINGS_COLLECTION)

@Singleton
class IncidentRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : IncidentRemoteDataSource {

    override suspend fun upsert(incident: Incident): Result<Incident> = runCatching {
        var finalIncident = incident
        if (incident.localImageUris.isNotEmpty()) {
            val urls = uploadImages(incident.localImageUris, incident.id, incident.type)
            finalIncident = incident.copy(
                evidencePhotoUrls = incident.evidencePhotoUrls + urls,
                localImageUris = emptyList()
            )
        }
        // Branches by type so SOS/SIGHTING actually land in their own collection instead of
        // silently collapsing into a generic "incidents" doc - this is the fix for the bug
        // documented in GRANT-READINESS-REPORT.md §1 ("SOS mirrors Incident Report").
        firestore.collection(collectionFor(finalIncident.type))
            .document(finalIncident.id)
            .set(finalIncident.toFirestoreMap())
            .await()
        finalIncident
    }

    private suspend fun uploadImages(
        localUris: List<String>,
        incidentId: String,
        type: IncidentType,
    ): List<String> {
        val urls = mutableListOf<String>()
        val collection = collectionFor(type)
        for ((index, uriString) in localUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${index}_${System.currentTimeMillis()}.jpg"
                // Path prefix matches the Firestore collection this incident's document will
                // live in (see storage.rules, whose read rule looks the document up under the
                // same collection name) so evidence-photo access scoping stays correct for
                // SOS/sighting reports too, not just plain incidents.
                val ref = storage.reference.child("$collection/$incidentId/$fileName")
                ref.putFile(uri).await()
                urls += ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload image %d for incident %s", index, incidentId)
            }
        }
        return urls
    }

    // Listens on all three collections so a teammate's SOS alert or wildlife sighting shows up
    // live on this device exactly like a plain incident report does - before the §1 fix this
    // only ever listened on "incidents", so once SOS/sightings started being written to their
    // own collections they would otherwise have stopped appearing in this realtime stream
    // entirely. One registration per collection, all forwarding into the same flow; all three
    // are torn down together when the flow is cancelled.
    override fun observeChanges(): Flow<RemoteIncidentChange> = callbackFlow {
        val registrations = ALL_COLLECTIONS.map { collectionName ->
            val query = firestore.collection(collectionName).orderBy("reportedAt", Query.Direction.DESCENDING)
            query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "%s snapshot listener error", collectionName)
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
        }
        awaitClose { registrations.forEach { it.remove() } }
    }
}
