package com.wildwatch.app.domain.model

import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.data.local.db.ClaimEntity
import com.wildwatch.app.data.local.db.ClaimStatus
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.SyncStatus

// Mirrors Incident.kt's role: the one canonical mapping between Room's
// ClaimEntity, this domain model, and the Firestore 'claims' document shape.
data class Claim(
    val id: String,
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
    val relatedIncidentId: String? = null,
    val evidencePhotoUrls: List<String> = emptyList(),
    val localImageUris: List<String> = emptyList(),
    val syncStatus: SyncStatus,
    val syncedAt: String? = null,
    val lastModified: Long,
) {
    val hasEvidence: Boolean get() = evidencePhotoUrls.isNotEmpty()
    val evidenceCount: Int get() = evidencePhotoUrls.size

    fun toEntity(): ClaimEntity = ClaimEntity(
        id = id,
        category = category,
        status = status,
        park = park,
        description = description,
        lat = lat,
        lng = lng,
        locationName = locationName,
        userName = userName,
        userEmail = userEmail,
        userId = userId,
        filedAt = filedAt,
        relatedIncidentId = relatedIncidentId,
        hasEvidence = hasEvidence,
        evidenceCount = evidenceCount,
        evidencePhotoUrls = evidencePhotoUrls,
        localImageUris = localImageUris,
        syncStatus = syncStatus,
        syncedAt = syncedAt,
        lastModified = lastModified,
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "category" to category.name.lowercase(),
        "status" to status.name.lowercase(),
        "park" to park.name,
        "description" to description,
        "lat" to lat,
        "lng" to lng,
        "locationName" to locationName,
        "userName" to userName,
        "userEmail" to userEmail,
        "userId" to userId,
        "filedAt" to filedAt,
        "relatedIncidentId" to relatedIncidentId,
        "synced" to true,
        "syncedAt" to syncedAt,
        "hasEvidence" to hasEvidence,
        "evidenceCount" to evidenceCount,
        "evidencePhotoUrls" to evidencePhotoUrls,
    )

    companion object {
        fun fromEntity(entity: ClaimEntity): Claim = Claim(
            id = entity.id,
            category = entity.category,
            status = entity.status,
            park = entity.park,
            description = entity.description,
            lat = entity.lat,
            lng = entity.lng,
            locationName = entity.locationName,
            userName = entity.userName,
            userEmail = entity.userEmail,
            userId = entity.userId,
            filedAt = entity.filedAt,
            relatedIncidentId = entity.relatedIncidentId,
            evidencePhotoUrls = entity.evidencePhotoUrls,
            localImageUris = entity.localImageUris,
            syncStatus = entity.syncStatus,
            syncedAt = entity.syncedAt,
            lastModified = entity.lastModified,
        )

        private fun parseCategory(data: Map<String, Any?>): ClaimCategory =
            when ((data["category"] as? String)?.lowercase()) {
                "crop_destruction" -> ClaimCategory.CROP_DESTRUCTION
                "livestock_predation" -> ClaimCategory.LIVESTOCK_PREDATION
                "property_damage" -> ClaimCategory.PROPERTY_DAMAGE
                "human_injury" -> ClaimCategory.HUMAN_INJURY
                "human_death" -> ClaimCategory.HUMAN_DEATH
                else -> ClaimCategory.OTHER_LOSS
            }

        private fun parseStatus(data: Map<String, Any?>): ClaimStatus =
            when ((data["status"] as? String)?.lowercase()) {
                "verified" -> ClaimStatus.VERIFIED
                "approved" -> ClaimStatus.APPROVED
                "paid" -> ClaimStatus.PAID
                "rejected" -> ClaimStatus.REJECTED
                else -> ClaimStatus.UNDER_VERIFICATION
            }

        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreDocument(documentId: String, data: Map<String, Any?>): Claim = Claim(
            id = documentId,
            category = parseCategory(data),
            status = parseStatus(data),
            park = Park.entries.find { it.name == data["park"] as? String } ?: Park.BWINDI_IMPENETRABLE,
            description = data["description"] as? String,
            lat = (data["lat"] as? Number)?.toDouble(),
            lng = (data["lng"] as? Number)?.toDouble(),
            locationName = data["locationName"] as? String,
            userName = data["userName"] as? String,
            userEmail = data["userEmail"] as? String,
            userId = data["userId"] as? String,
            filedAt = data["filedAt"] as? String ?: "",
            relatedIncidentId = data["relatedIncidentId"] as? String,
            evidencePhotoUrls = (data["evidencePhotoUrls"] as? List<String>) ?: emptyList(),
            localImageUris = emptyList(),
            syncStatus = SyncStatus.SYNCED,
            syncedAt = data["syncedAt"] as? String,
            lastModified = System.currentTimeMillis(),
        )
    }
}
