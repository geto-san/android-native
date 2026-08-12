package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRemoteDataSource {
    // Returns the incident actually written to Firestore, evidencePhotoUrls/localImageUris
    // included - the caller uploaded any pending local images as part of this call, and that
    // is the only place the resulting Storage URLs are known.
    suspend fun upsert(incident: Incident): Result<Incident>
    fun observeChanges(): Flow<RemoteIncidentChange>
}
