package com.wildwatch.app.core.data.bridge

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Mobile-direct counterpart of the (now Blaze-only) Cloud-Functions-relayed webhooks
// (web-portal/backend/routes/api.php's webhooks/* group) - see that file's mobile/* group.
// Bodies are built and serialized by LaravelBridgeDataSourceImpl rather than a Retrofit
// converter, since the payload is the same loosely-typed Firestore-document shape
// Incident.toFirestoreMap() already produces; no ConverterFactory is needed when every
// endpoint here only uses raw RequestBody/ResponseBody.
interface LaravelBridgeApi {

    @POST("mobile/incidents")
    suspend fun postIncidentEvent(
        @Header("Authorization") bearerToken: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}
