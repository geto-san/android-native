package com.wildwatch.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mirrors the Firestore 'incidents' document shape field-for-field (see
// android-native/README.md), plus local-only bookkeeping:
// - id is a client-generated UUID, set at creation time (guardrail G4) - it is
//   both the Room primary key AND the Firestore document ID once synced, so
//   dedup is an ID-equality problem rather than a timestamp heuristic.
// - syncStatus drives the G3 sync state machine; it is NOT part of the Firestore
//   document (Firestore gets a plain 'synced: true' boolean derived from it).
// - localImageUris holds on-device file paths for evidence photos not yet
//   uploaded; evidencePhotoUrls holds the Storage download URLs once they are.
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val type: IncidentType,
    val status: IncidentStatus,
    val rangerProgress: RangerProgress?,
    val isEscalated: Boolean,
    val park: Park,
    val community: String,
    val species: String,
    val severity: Severity,
    val category: String?,
    val summary: String?,
    val lat: Double,
    val lng: Double,
    val locationName: String?,
    val userName: String?,
    val userEmail: String?,
    val userId: String?,
    val reportedAt: String,
    val assignedTo: String?,
    val assignedToName: String?,
    val hasEvidence: Boolean,
    val evidenceCount: Int,
    val evidencePhotoUrls: List<String>,
    val localImageUris: List<String>,
    val voiceNoteUrl: String?,
    val voiceNoteDurationSec: Int?,
    val syncStatus: SyncStatus,
    val syncedAt: String?,
    val lastModified: Long,
)
