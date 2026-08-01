package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.IncidentEntity
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.RangerProgress
import com.wildwatch.app.core.database.SyncStatus

// The one canonical mapping between Room's IncidentEntity, this domain model,
// and the Firestore document shape (documented in android-native/README.md).
// Centralizing it here is a deliberate fix for the exact class of bug the RN app
// had: two different code paths (online-create vs. background-sync) wrote to
// Firestore using two different field-naming schemes because there was no single
// place defining "this is what an incident document looks like."
data class Incident(
    val id: String,
    val type: IncidentType,
    val status: IncidentStatus,
    val rangerProgress: RangerProgress? = null,
    val isEscalated: Boolean = false,
    val park: Park,
    val district: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val community: String,
    val species: String,
    val severity: IncidentSeverity,
    val category: String? = null,
    val summary: String? = null,
    val lat: Double,
    val lng: Double,
    val locationName: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userId: String? = null,
    val reportedAt: String,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val evidencePhotoUrls: List<String> = emptyList(),
    val localImageUris: List<String> = emptyList(),
    val voiceNoteUrl: String? = null,
    val voiceNoteDurationSec: Int? = null,
    val syncStatus: SyncStatus,
    val syncedAt: String? = null,
    val lastModified: Long,
) {
    // Derived, not stored - evidencePhotoUrls is the single source of truth for
    // whether this incident has evidence photos, so hasEvidence/evidenceCount can
    // never drift out of sync with the actual list (they're re-derived on every
    // entity/Firestore write instead).
    val hasEvidence: Boolean get() = evidencePhotoUrls.isNotEmpty()
    val evidenceCount: Int get() = evidencePhotoUrls.size

    fun toEntity(): IncidentEntity = IncidentEntity(
        id = id,
        type = type,
        status = status,
        rangerProgress = rangerProgress,
        isEscalated = isEscalated,
        park = park,
        district = district,
        subCounty = subCounty,
        parish = parish,
        community = community,
        species = species,
        severity = severity,
        category = category,
        summary = summary,
        lat = lat,
        lng = lng,
        locationName = locationName,
        userName = userName,
        userEmail = userEmail,
        userId = userId,
        reportedAt = reportedAt,
        assignedTo = assignedTo,
        assignedToName = assignedToName,
        hasEvidence = hasEvidence,
        evidenceCount = evidenceCount,
        evidencePhotoUrls = evidencePhotoUrls,
        localImageUris = localImageUris,
        voiceNoteUrl = voiceNoteUrl,
        voiceNoteDurationSec = voiceNoteDurationSec,
        syncStatus = syncStatus,
        syncedAt = syncedAt,
        lastModified = lastModified,
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "type" to type.name.lowercase(),
        "status" to status.name.lowercase(),
        "rangerProgress" to rangerProgress?.name?.lowercase(),
        "isEscalated" to isEscalated,
        "park" to park.name,
        "district" to district,
        "sub_county" to subCounty,
        "parish" to parish,
        "community" to community,
        "species" to species,
        "severity" to severity.name.lowercase(),
        "category" to category,
        "summary" to summary,
        "lat" to lat,
        "lng" to lng,
        "locationName" to locationName,
        "userName" to userName,
        "userEmail" to userEmail,
        "userId" to userId,
        "reportedAt" to reportedAt,
        "synced" to true,
        "syncedAt" to syncedAt,
        "assignedTo" to assignedTo,
        "assignedToName" to assignedToName,
        "hasEvidence" to hasEvidence,
        "evidenceCount" to evidenceCount,
        "evidencePhotoUrls" to evidencePhotoUrls,
        "voiceNoteUrl" to voiceNoteUrl,
        "voiceNoteDurationSec" to voiceNoteDurationSec,
    )

    companion object {
        fun fromEntity(entity: IncidentEntity): Incident = Incident(
            id = entity.id,
            type = entity.type,
            status = entity.status,
            rangerProgress = entity.rangerProgress,
            isEscalated = entity.isEscalated,
            park = entity.park,
            district = entity.district,
            subCounty = entity.subCounty,
            parish = entity.parish,
            community = entity.community,
            species = entity.species,
            severity = entity.severity,
            category = entity.category,
            summary = entity.summary,
            lat = entity.lat,
            lng = entity.lng,
            locationName = entity.locationName,
            userName = entity.userName,
            userEmail = entity.userEmail,
            userId = entity.userId,
            reportedAt = entity.reportedAt,
            assignedTo = entity.assignedTo,
            assignedToName = entity.assignedToName,
            evidencePhotoUrls = entity.evidencePhotoUrls,
            localImageUris = entity.localImageUris,
            voiceNoteUrl = entity.voiceNoteUrl,
            voiceNoteDurationSec = entity.voiceNoteDurationSec,
            syncStatus = entity.syncStatus,
            syncedAt = entity.syncedAt,
            lastModified = entity.lastModified,
        )

        // Maps an incoming Firestore document (e.g. a teammate's incident, or this
        // repository's own doc read back) into a domain Incident. The Firestore
        // document ID becomes this row's id (guardrail G4 - id and Firestore doc ID
        // are always the same value, in both directions), and it always arrives
        // already SYNCED since it came from the server.
        private fun parseType(data: Map<String, Any?>): IncidentType = when ((data["type"] as? String)?.lowercase()) {
            "conflict" -> IncidentType.CONFLICT
            "emergency" -> IncidentType.EMERGENCY
            "poaching" -> IncidentType.POACHING
            else -> IncidentType.SIGHTING
        }

        private fun parseStatus(data: Map<String, Any?>): IncidentStatus =
            when ((data["status"] as? String)?.lowercase()) {
                "in_progress" -> IncidentStatus.IN_PROGRESS
                "resolved" -> IncidentStatus.RESOLVED
                else -> IncidentStatus.OPEN
            }

        private fun parseRangerProgress(data: Map<String, Any?>): RangerProgress? =
            when ((data["rangerProgress"] as? String)?.lowercase()) {
                "en_route" -> RangerProgress.EN_ROUTE
                "on_site" -> RangerProgress.ON_SITE
                else -> null
            }

        private fun parseSeverity(data: Map<String, Any?>): IncidentSeverity =
            when ((data["severity"] as? String)?.lowercase()) {
                "low" -> IncidentSeverity.LOW
                "high" -> IncidentSeverity.HIGH
                "light" -> IncidentSeverity.LIGHT
                else -> IncidentSeverity.MEDIUM
            }

        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreDocument(documentId: String, data: Map<String, Any?>): Incident = Incident(
            id = documentId,
            type = parseType(data),
            status = parseStatus(data),
            rangerProgress = parseRangerProgress(data),
            isEscalated = data["isEscalated"] as? Boolean ?: false,
            park = Park.entries.find { it.name == data["park"] as? String } ?: Park.BWINDI_IMPENETRABLE,
            district = data["district"] as? String,
            subCounty = data["sub_county"] as? String,
            parish = data["parish"] as? String,
            community = data["community"] as? String ?: "",
            species = data["species"] as? String ?: "",
            severity = parseSeverity(data),
            category = data["category"] as? String,
            summary = data["summary"] as? String,
            lat = (data["lat"] as? Number)?.toDouble() ?: 0.0,
            lng = (data["lng"] as? Number)?.toDouble() ?: 0.0,
            locationName = data["locationName"] as? String,
            userName = data["userName"] as? String,
            userEmail = data["userEmail"] as? String,
            userId = data["userId"] as? String,
            reportedAt = data["reportedAt"] as? String ?: "",
            assignedTo = data["assignedTo"] as? String,
            assignedToName = data["assignedToName"] as? String,
            evidencePhotoUrls = (data["evidencePhotoUrls"] as? List<String>) ?: emptyList(),
            localImageUris = emptyList(),
            voiceNoteUrl = data["voiceNoteUrl"] as? String,
            voiceNoteDurationSec = (data["voiceNoteDurationSec"] as? Number)?.toInt(),
            syncStatus = SyncStatus.SYNCED,
            syncedAt = data["syncedAt"] as? String,
            lastModified = System.currentTimeMillis(),
        )
    }
}
