package com.silversentry.sentry.core.data.bridge

import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.model.Incident
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

// Keys never mirrored to the Laravel side. "answers" holds a parsed kotlinx JsonElement
// (see Incident.toFirestoreMap) that the bridge payload doesn't need - FirestoreSyncMapper
// never reads it - so it is dropped rather than converted (this is the same filtering
// decision the previous org.json-based builder made, minus the org.json dependency).
private val BRIDGE_EXCLUDED_KEYS = setOf("answers")

@Singleton
class LaravelBridgeDataSourceImpl @Inject constructor(
    private val api: LaravelBridgeApi,
    private val authRepository: AuthRepository,
) : LaravelBridgeDataSource {

    override suspend fun postIncidentEvent(incident: Incident, eventType: String): Result<Unit> = runCatching {
        val token = authRepository.getIdToken()
            ?: error("No Firebase ID token available; user is signed out")

        // Built with kotlinx.serialization (the JSON engine this app already uses and the one
        // mihon's network layer serializes with) rather than org.json: org.json is an
        // Android-framework stub in JVM unit tests (it throws instead of building a payload),
        // so no test could ever exercise the real routing logic below, and its Map-wrapping
        // constructor doesn't understand the firestore document's JsonElement values anyway.
        val payload = buildJsonObject {
            put("docId", incident.id)
            put("eventType", eventType)
            put("after", incident.toBridgePayloadMap())
        }
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        // Route to the Laravel bridge endpoint that matches this incident's type. Previously
        // every type - including SOS and SIGHTING - always hit mobile/incidents, so those two
        // never reached SosAlertController/WildlifeSighting's own tables even though the
        // mobile/sos-alerts and mobile/sightings routes already existed server-side. This is
        // the fix for that gap.
        val response = when (incident.type) {
            IncidentType.SOS -> api.postSosAlertEvent("Bearer $token", body)
            IncidentType.SIGHTING -> api.postSightingEvent("Bearer $token", body)
            else -> api.postIncidentEvent("Bearer $token", body)
        }
        if (!response.isSuccessful) {
            throw java.io.IOException("Laravel bridge call failed: HTTP ${response.code()}")
        }
    }

    private fun Incident.toBridgePayloadMap(): JsonObject {
        val values = toFirestoreMap().filterKeys { it !in BRIDGE_EXCLUDED_KEYS }
        return JsonObject(values.mapValues { (_, value) -> value.toBridgeJsonElement() })
    }

    private fun Any?.toBridgeJsonElement(): kotlinx.serialization.json.JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this.toDouble())
        is List<*> -> JsonArray(this.map { it.toBridgeJsonElement() })
        is Map<*, *> -> JsonObject(
            this.entries.associate { (key, value) -> key.toString() to value.toBridgeJsonElement() },
        )
        // Anything unrepresentable (e.g. a parsed JsonElement that slipped past the filter)
        // is dropped as null rather than risking payload rejection.
        else -> JsonNull
    }
}
