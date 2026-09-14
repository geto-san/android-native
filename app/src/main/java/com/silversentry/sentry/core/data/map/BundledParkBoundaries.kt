package com.silversentry.sentry.core.data.map

import android.content.Context
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Offline fallback for the ranger tracking map's park boundary overlay (borrowed from the
// wwmap offline-boundaries pattern). RangerTrackingScreen prefers the live Firestore
// boundary_geojson when the active park carries one; this repository supplies the
// app-bundled parks.geojson copy so the outline still renders before any Firestore cache
// is populated - i.e. on a field ranger's very first frame offline.
//
// The asset is generated from scripts/generate_parks_geojson.mjs, which mirrors the
// backend seed script's boundary formula exactly.
@Singleton
class BundledParkBoundaries @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ringsByParkId: Map<String, List<List<Point>>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        parseBundledParks(readAsset())
    }

    fun ringsFor(parkId: String): List<List<Point>> = ringsByParkId[parkId] ?: emptyList()

    private fun readAsset(): String =
        runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.onFailure { e -> Timber.w(e, "Bundled parks GeoJSON missing: %s", ASSET_NAME) }
            .getOrDefault("")

    companion object {
        const val ASSET_NAME = "parks.geojson"
    }
}

// Top-level plumbing kept out of the class so the plain-JVM unit test can exercise the
// same parsing that runs on-device (mirrors MapOfflineRepositoryTest's parseBoundaryGeometry).
internal fun parseBundledParks(json: String): Map<String, List<List<Point>>> {
    if (json.isBlank()) return emptyMap()
    return runCatching {
        FeatureCollection.fromJson(json).features()
            ?.filterNotNull()
            ?.mapNotNull { feature ->
                val id = feature.id()?.takeIf { it.isNotBlank() }
                    ?: feature.properties()?.getAsJsonPrimitive("id")?.asString?.takeIf { it.isNotBlank() }
                val rings = geometryToRings(feature.geometry()).takeIf { it.isNotEmpty() }
                if (id == null || rings == null) null else id to rings
            }
            ?.toMap()
            ?: emptyMap()
    }.getOrElse { e ->
        Timber.w(e, "Failed to parse bundled parks GeoJSON")
        emptyMap()
    }
}

internal fun geometryToRings(geometry: Geometry?): List<List<Point>> = when (geometry) {
    is Polygon -> geometry.coordinates()
    is MultiPolygon -> geometry.polygons().flatMap { it.coordinates() }
    else -> emptyList()
}