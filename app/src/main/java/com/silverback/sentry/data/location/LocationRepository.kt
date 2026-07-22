package com.silverback.sentry.data.location

data class GeoLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Float?)

// Per guardrail G7, this is the only location surface the UI layer depends on.
// The runtime CAMERA/LOCATION permission requests themselves stay in the UI
// layer (they need an Activity/permission launcher), but the actual
// FusedLocationProviderClient/Geocoder calls happen only here - the caller is
// responsible for confirming permission is granted before calling this.
interface LocationRepository {
    suspend fun getCurrentLocation(): Result<GeoLocation>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?
}
