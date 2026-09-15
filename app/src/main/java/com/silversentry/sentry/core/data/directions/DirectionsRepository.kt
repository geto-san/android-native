package com.silversentry.sentry.core.data.directions

import com.mapbox.geojson.Point
import com.silversentry.sentry.core.data.location.GeoLocation

// A resolved route between two points plus its trip summary, as returned by the Mapbox
// Directions API and used by the ranger incident navigation flow (Google-Maps-style
// "Your location -> Destination" preview on the detail screen and the full-screen
// navigation view that follows it).
data class DrivingRoute(
    val points: List<Point>,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

interface DirectionsRepository {
    /** Driving route + summary from origin to destination. Returns a failure result if the
     *  Mapbox token is unconfigured at build time, the API errors, or no route exists. */
    suspend fun getDrivingRoute(origin: GeoLocation, destination: GeoLocation): Result<DrivingRoute>
}
