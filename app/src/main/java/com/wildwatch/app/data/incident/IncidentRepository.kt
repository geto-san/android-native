package com.wildwatch.app.data.incident

import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.domain.model.Incident
import kotlinx.coroutines.flow.Flow

data class SyncResult(val succeeded: Int, val failed: Int)

// Everything a new incident report needs to supply, bundled into one object
// rather than an 11-parameter create() call.
data class NewIncidentDetails(
    val type: IncidentType,
    val park: Park,
    val community: String,
    val species: String,
    val severity: Severity,
    val category: String?,
    val summary: String?,
    val lat: Double,
    val lng: Double,
    val locationName: String?,
    val localImageUris: List<String>,
)

// Per guardrail G7, this is the only incident data surface the UI layer is
// allowed to depend on. Per guardrail G1, IncidentRepositoryImpl is the only
// implementation, and the only class that talks to IncidentRemoteDataSource.
interface IncidentRepository {

    // Reads always come from Room (guardrail G2) - never a standalone Firestore
    // read. Live Firestore snapshots are folded into what this Flow emits, but
    // the UI never sees Firestore data that hasn't landed in Room first.
    fun observeAll(): Flow<List<Incident>>

    // Inserts locally as OPEN and returns immediately (guardrail G2: Room
    // first, always). Does not itself attempt a network sync - callers (a
    // ViewModel checking connectivity, or the WorkManager job) decide when to
    // call syncPending().
    suspend fun create(details: NewIncidentDetails): Incident

    // Claims an unassigned incident for the current ranger: OPEN -> IN_PROGRESS,
    // assignedTo/assignedToName set, rangerProgress starts at EN_ROUTE. Updates
    // Room immediately; if the row has already synced, also pushes a lightweight
    // update to the existing Firestore document (mirrors the pattern used for
    // syncPending()'s own writes - an unsynced row's assignment rides along in
    // its normal syncPending() write instead of a separate remote call).
    suspend fun assignToSelf(id: String)

    // The one atomic, single-writer sync function (guardrail G3). Safe to call
    // concurrently or repeatedly - a second call while one is already running
    // is a no-op rather than a duplicate pass.
    suspend fun syncPending(): SyncResult

    // Starts folding live Firestore changes into Room (guardrail G2/G5). Safe to
    // call more than once - only the first call actually starts a listener.
    // Called once from WildWatchApplication.onCreate(), not from any screen.
    fun startObservingRemoteChanges()
}
