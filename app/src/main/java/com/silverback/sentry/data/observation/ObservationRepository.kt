package com.silverback.sentry.data.observation

import com.silverback.sentry.domain.model.Observation
import kotlinx.coroutines.flow.Flow

data class SyncResult(val succeeded: Int, val failed: Int)

// Per guardrail G7, this is the only observation data surface the UI layer is
// allowed to depend on. Per guardrail G1, ObservationRepositoryImpl is the only
// implementation, and the only class that talks to ObservationRemoteDataSource.
interface ObservationRepository {

    // Reads always come from Room (guardrail G2) - never a standalone Firestore
    // read. Phase 7 folds live Firestore snapshots into what this Flow emits,
    // but the UI never sees Firestore data that hasn't landed in Room first.
    fun observeAll(): Flow<List<Observation>>

    // Inserts locally as PENDING and returns immediately (guardrail G2: Room
    // first, always). Does not itself attempt a network sync - callers (a
    // ViewModel checking connectivity, or the Phase 6 WorkManager job) decide
    // when to call syncPending().
    suspend fun create(
        gorillaGroup: String,
        location: String,
        locationName: String?,
        healthStatus: String?,
        notes: String?,
        localImageUris: List<String>,
    ): Observation

    // Updates the local row immediately; if it has already synced, also pushes
    // a lightweight update to the existing Firestore document. If it hasn't
    // synced yet, the attended fields ride along in the eventual syncPending()
    // write instead of needing a second remote call.
    suspend fun markAttended(id: String)

    // The one atomic, single-writer sync function (guardrail G3). Safe to call
    // concurrently or repeatedly - a second call while one is already running
    // is a no-op rather than a duplicate pass.
    suspend fun syncPending(): SyncResult

    // Starts folding live Firestore changes into Room (guardrail G2/G5). Safe to
    // call more than once - only the first call actually starts a listener.
    // Called once from SentryApplication.onCreate(), not from any screen.
    fun startObservingRemoteChanges()
}
