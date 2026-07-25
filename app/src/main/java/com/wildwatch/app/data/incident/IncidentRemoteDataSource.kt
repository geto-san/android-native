package com.wildwatch.app.data.incident

import kotlinx.coroutines.flow.Flow

// Thin wrapper around Firestore/Storage calls, deliberately kept separate from
// IncidentRepositoryImpl so unit tests can fake it without touching real
// Firebase (see IncidentRepositoryImplTest). Per guardrail G1,
// IncidentRepositoryImpl is the only class that depends on this interface -
// no ViewModel or Composable does.
interface IncidentRemoteDataSource {

    // Uploads each local file URI to Storage under incidents/{incidentId}/,
    // returning the download URLs in the same order. A single photo failing to
    // upload is logged and skipped rather than failing the whole batch (matches
    // the fixed RN behavior).
    suspend fun uploadImages(localUris: List<String>, incidentId: String): List<String>

    // Writes (creates or overwrites) the incident document at the given id.
    // Per guardrail G4, id is always the client-generated UUID - this never lets
    // Firestore auto-generate a different document ID than the local row's id.
    suspend fun writeDocument(id: String, data: Map<String, Any?>)

    // Live Firestore snapshot listener, emitting each batch of changes as they
    // arrive. IncidentRepositoryImpl folds these into Room (guardrail G2);
    // nothing else ever reads this directly.
    fun observeChanges(): Flow<List<RemoteIncidentChange>>
}
