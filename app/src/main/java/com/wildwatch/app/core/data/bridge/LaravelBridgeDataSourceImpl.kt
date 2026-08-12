package com.wildwatch.app.core.data.bridge

import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.model.Incident
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@Singleton
class LaravelBridgeDataSourceImpl @Inject constructor(
    private val api: LaravelBridgeApi,
    private val authRepository: AuthRepository,
) : LaravelBridgeDataSource {

    override suspend fun postIncidentEvent(incident: Incident, eventType: String): Result<Unit> = runCatching {
        val token = authRepository.getIdToken()
            ?: throw IllegalStateException("No Firebase ID token available; user is signed out")

        val payload = JSONObject().apply {
            put("docId", incident.id)
            put("eventType", eventType)
            // "answers" holds a parsed JsonElement (see Incident.toFirestoreMap) that org.json's
            // Map-wrapping constructor doesn't understand, and FirestoreSyncMapper on the Laravel
            // side never reads it anyway - dropped rather than converted.
            put("after", JSONObject(incident.toFirestoreMap().filterKeys { it != "answers" }))
        }
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val response = api.postIncidentEvent("Bearer $token", body)
        if (!response.isSuccessful) {
            throw java.io.IOException("Laravel bridge call failed: HTTP ${response.code()}")
        }
    }
}
