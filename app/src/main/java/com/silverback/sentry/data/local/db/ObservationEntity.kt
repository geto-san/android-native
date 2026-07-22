package com.silverback.sentry.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mirrors the Firestore 'observations' document shape field-for-field (see
// android-native/README.md and the migration plan), plus local-only bookkeeping:
// - id is a client-generated UUID, set at creation time (guardrail G4) - it is
//   both the Room primary key AND the Firestore document ID once synced, so
//   dedup is an ID-equality problem rather than a timestamp heuristic.
// - syncStatus drives the G3 sync state machine; it is NOT part of the Firestore
//   document (Firestore gets a plain 'synced: true' boolean derived from it).
// - localImageUris holds on-device file paths for photos not yet uploaded;
//   imageUrls holds the Storage download URLs once they are.
@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val gorillaGroup: String,
    val location: String,
    val locationName: String?,
    val healthStatus: String?,
    val notes: String?,
    val userName: String?,
    val userEmail: String?,
    val userId: String?,
    val createdAt: String,
    val observationStatus: ObservationStatus,
    val attendedAt: String?,
    val attendedBy: String?,
    val attendedByName: String?,
    val hasImages: Boolean,
    val imageCount: Int,
    val imageUrls: List<String>,
    val localImageUris: List<String>,
    val syncStatus: SyncStatus,
    val syncedAt: String?,
    val lastModified: Long,
)
