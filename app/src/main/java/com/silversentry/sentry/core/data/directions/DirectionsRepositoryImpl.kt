package com.silversentry.sentry.core.data.directions

import com.mapbox.geojson.Point
import com.silversentry.sentry.BuildConfig
import com.silversentry.sentry.core.data.location.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class DirectionsResponse(val routes: List<DirectionsRoute> = emptyList())

@Serializable
private data class DirectionsRoute(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: DirectionsGeometry = DirectionsGeometry(),
)

// GeoJSON LineString coordinate pairs - note the API returns [longitude, latitude] order.
@Serializable
private data class DirectionsGeometry(val coordinates: List<List<Double>> = emptyList())

// Maps the Google-Maps-style "You -> destination" trip preview and the full navigation
// view onto the driving Directions API. Kept deliberately separate from the centerpiece
// Retrofit/Laravel-bridge client (see NetworkModule) - this is a plain Mapbox HTTP call
// scoped to routing, with its own DTOs and a single DirectionsRepository consumer.
@Singleton
class DirectionsRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : DirectionsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getDrivingRoute(
        origin: GeoLocation,
        destination: GeoLocation,
    ): Result<DrivingRoute> = withContext(Dispatchers.IO) {
        runCatching {
            val token = BuildConfig.MAPBOX_ACCESS_TOKEN
            check(token.isNotBlank() && !token.startsWith("YOUR_")) { "Mapbox token not configured" }

            val coordinates =
                "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
            val url =
                buildString {
                    append(BASE_URL).append("/driving/").append(coordinates)
                    append("?geometries=geojson&overview=full&steps=false&access_token=$token")
                }

            val request = Request.Builder().url(url).build()
            val body = okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Directions API responded ${response.code}" }
                checkNotNull(response.body) { "Directions API returned no body" }.string()
            }

            val route = json.decodeFromString<DirectionsResponse>(body).routes.firstOrNull()
                ?: error("No driving route found")
            DrivingRoute(
                points = route.geometry.coordinates.map { (lng, lat) -> Point.fromLngLat(lng, lat) },
                distanceMeters = route.distance,
                durationSeconds = route.duration,
            )
        }
    }

    private companion object {
        const val BASE_URL = "https://api.mapbox.com/directions/v5/mapbox"
    }
}
