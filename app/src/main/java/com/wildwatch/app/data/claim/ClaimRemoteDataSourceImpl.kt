package com.wildwatch.app.data.claim

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

private const val CLAIMS_COLLECTION = "claims"

// Mirrors IncidentRemoteDataSourceImpl - see its own doc comment.
@Singleton
class ClaimRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : ClaimRemoteDataSource {

    override suspend fun uploadImages(localUris: List<String>, claimId: String): List<String> {
        if (localUris.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()
        for ((index, uriString) in localUris.withIndex()) {
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${index}_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child("$CLAIMS_COLLECTION/$claimId/$fileName")
                ref.putFile(uri).await()
                urls += ref.downloadUrl.await().toString()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                Timber.e(e, "Failed to upload image %d for claim %s", index, claimId)
            }
        }
        return urls
    }

    override suspend fun writeDocument(id: String, data: Map<String, Any?>) {
        firestore.collection(CLAIMS_COLLECTION).document(id).set(data).await()
    }

    override fun observeChanges(): Flow<List<RemoteClaimChange>> = callbackFlow {
        val query = firestore.collection(CLAIMS_COLLECTION).orderBy("filedAt", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Claims snapshot listener error")
                return@addSnapshotListener
            }
            val changes = snapshot?.documentChanges?.map { change ->
                val id = change.document.id
                val data = change.document.data
                when (change.type) {
                    DocumentChange.Type.ADDED -> RemoteClaimChange.Added(id, data)
                    DocumentChange.Type.MODIFIED -> RemoteClaimChange.Modified(id, data)
                    DocumentChange.Type.REMOVED -> RemoteClaimChange.Removed(id)
                }
            } ?: return@addSnapshotListener
            trySend(changes)
        }
        awaitClose { registration.remove() }
    }
}
