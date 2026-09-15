package com.silversentry.sentry.feature.incidentdetail

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max

internal fun TripCamera.centerFor(a: Point, b: Point): Point =
    Point.fromLngLat((a.longitude() + b.longitude()) / 2.0, (a.latitude() + b.latitude()) / 2.0)

internal fun TripCamera.zoomFor(a: Point, b: Point): Double = zoomForSpan(
    lat = (a.latitude() + b.latitude()) / 2.0,
    spanLngDeg = abs(a.longitude() - b.longitude()),
)

internal fun TripCamera.zoomForSpan(lat: Double, spanLngDeg: Double): Double {
    val worldSpanPx = 512.0
    val degPerPx = 360.0 / worldSpanPx
    val spanPx = max(spanLngDeg / degPerPx * cos(Math.toRadians(lat)), 1.0)
    val zoom = ln(worldSpanPx / spanPx) / ln(2.0)
    return zoom.coerceIn(8.0, 16.0)
}

internal fun TripCamera.optionsFor(points: List<Point>): CameraOptions {
    val minLat = points.minOf { it.latitude() }
    val maxLat = points.maxOf { it.latitude() }
    val minLng = points.minOf { it.longitude() }
    val maxLng = points.maxOf { it.longitude() }
    val center = Point.fromLngLat((minLng + maxLng) / 2.0, (minLat + maxLat) / 2.0)
    return CameraOptions.Builder()
        .center(center)
        .zoom(zoomForSpan(lat = center.latitude(), spanLngDeg = maxLng - minLng))
        .build()
}

// Seconds -> "~X min" when we have the driving ETA; otherwise a straight-line guess at a
// field-response pace isn't shown and we render the distance only.
internal fun formatEta(durationSeconds: Double?, distanceKm: Double?): String {
    val etaMinutes = durationSeconds
        ?.let { ceil(it / 60.0).toInt() }
    return when {
        etaMinutes != null && etaMinutes < 1 -> "<1 min"
        etaMinutes != null -> "~$etaMinutes min"
        distanceKm != null -> "~${ceil(distanceKm / 5.0).toInt().coerceAtLeast(1)} min"
        else -> "--"
    }
}

internal fun formatDistanceKm(distanceKm: Double?): String = when {
    distanceKm == null -> ""
    distanceKm < 1.0 -> "%.0f m".format(distanceKm * 1000.0)
    else -> "%.1f km".format(distanceKm)
}

// Marker type so the extension functions above read as `TripCamera.xxx(...)`.
internal object TripCamera
