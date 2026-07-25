package com.wildwatch.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mirrors IncidentEntity's shape/conventions (see its own doc comment): id is a
// client-generated UUID (guardrail G4), syncStatus drives the same G3 sync
// state machine, localImageUris/evidencePhotoUrls split the same way between
// not-yet-uploaded local files and Storage download URLs.
@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey val id: String,
    val category: ClaimCategory,
    val status: ClaimStatus,
    val park: Park,
    val description: String?,
    val lat: Double?,
    val lng: Double?,
    val locationName: String?,
    val userName: String?,
    val userEmail: String?,
    val userId: String?,
    val filedAt: String,
    val relatedIncidentId: String?,
    val hasEvidence: Boolean,
    val evidenceCount: Int,
    val evidencePhotoUrls: List<String>,
    val localImageUris: List<String>,
    val syncStatus: SyncStatus,
    val syncedAt: String?,
    val lastModified: Long,
)
