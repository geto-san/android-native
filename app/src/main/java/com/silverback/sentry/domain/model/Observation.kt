package com.silverback.sentry.domain.model

import com.silverback.sentry.data.local.db.ObservationEntity
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus

// The one canonical mapping between Room's ObservationEntity, this domain model,
// and the Firestore document shape (documented in android-native/README.md).
// Centralizing it here is a deliberate fix for the exact class of bug the RN app
// had: two different code paths (online-create vs. background-sync) wrote to
// Firestore using two different field-naming schemes because there was no single
// place defining "this is what an observation document looks like."
data class Observation(
    val id: String,
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
    val attendedAt: String? = null,
    val attendedBy: String? = null,
    val attendedByName: String? = null,
    val imageUrls: List<String> = emptyList(),
    val localImageUris: List<String> = emptyList(),
    val syncStatus: SyncStatus,
    val syncedAt: String? = null,
    val lastModified: Long,
) {
    // Derived, not stored - imageUrls is the single source of truth for whether
    // this observation has photos, so hasImages/imageCount can never drift out
    // of sync with the actual list (they're re-derived on every entity/Firestore
    // write instead).
    val hasImages: Boolean get() = imageUrls.isNotEmpty()
    val imageCount: Int get() = imageUrls.size

    fun toEntity(): ObservationEntity = ObservationEntity(
        id = id,
        gorillaGroup = gorillaGroup,
        location = location,
        locationName = locationName,
        healthStatus = healthStatus,
        notes = notes,
        userName = userName,
        userEmail = userEmail,
        userId = userId,
        createdAt = createdAt,
        observationStatus = observationStatus,
        attendedAt = attendedAt,
        attendedBy = attendedBy,
        attendedByName = attendedByName,
        hasImages = hasImages,
        imageCount = imageCount,
        imageUrls = imageUrls,
        localImageUris = localImageUris,
        syncStatus = syncStatus,
        syncedAt = syncedAt,
        lastModified = lastModified,
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "gorillaGroup" to gorillaGroup,
        "location" to location,
        "locationName" to locationName,
        "healthStatus" to healthStatus,
        "notes" to notes,
        "userName" to userName,
        "userEmail" to userEmail,
        "userId" to userId,
        "createdAt" to createdAt,
        "synced" to true,
        "syncedAt" to syncedAt,
        "status" to observationStatus.name.lowercase(),
        "attendedAt" to attendedAt,
        "attendedBy" to attendedBy,
        "attendedByName" to attendedByName,
        "hasImages" to hasImages,
        "imageCount" to imageCount,
        "imageUrls" to imageUrls,
    )

    companion object {
        fun fromEntity(entity: ObservationEntity): Observation = Observation(
            id = entity.id,
            gorillaGroup = entity.gorillaGroup,
            location = entity.location,
            locationName = entity.locationName,
            healthStatus = entity.healthStatus,
            notes = entity.notes,
            userName = entity.userName,
            userEmail = entity.userEmail,
            userId = entity.userId,
            createdAt = entity.createdAt,
            observationStatus = entity.observationStatus,
            attendedAt = entity.attendedAt,
            attendedBy = entity.attendedBy,
            attendedByName = entity.attendedByName,
            imageUrls = entity.imageUrls,
            localImageUris = entity.localImageUris,
            syncStatus = entity.syncStatus,
            syncedAt = entity.syncedAt,
            lastModified = entity.lastModified,
        )

        // Maps an incoming Firestore document (e.g. a teammate's sighting, or this
        // repository's own doc read back) into a domain Observation. The Firestore
        // document ID becomes this row's id (guardrail G4 - id and Firestore doc ID
        // are always the same value, in both directions), and it always arrives
        // already SYNCED since it came from the server.
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreDocument(documentId: String, data: Map<String, Any?>): Observation = Observation(
            id = documentId,
            gorillaGroup = data["gorillaGroup"] as? String ?: "",
            location = data["location"] as? String ?: "",
            locationName = data["locationName"] as? String,
            healthStatus = data["healthStatus"] as? String,
            notes = data["notes"] as? String,
            userName = data["userName"] as? String,
            userEmail = data["userEmail"] as? String,
            userId = data["userId"] as? String,
            createdAt = data["createdAt"] as? String ?: "",
            observationStatus = when ((data["status"] as? String)?.lowercase()) {
                "attended" -> ObservationStatus.ATTENDED
                else -> ObservationStatus.PENDING
            },
            attendedAt = data["attendedAt"] as? String,
            attendedBy = data["attendedBy"] as? String,
            attendedByName = data["attendedByName"] as? String,
            imageUrls = (data["imageUrls"] as? List<String>) ?: emptyList(),
            localImageUris = emptyList(),
            syncStatus = SyncStatus.SYNCED,
            syncedAt = data["syncedAt"] as? String,
            lastModified = System.currentTimeMillis(),
        )
    }
}
