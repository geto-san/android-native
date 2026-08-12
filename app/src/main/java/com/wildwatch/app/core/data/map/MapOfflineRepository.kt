package com.wildwatch.app.core.data.map

import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Geometry
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.wildwatch.app.core.model.NationalPark
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The app's default (non-satellite) map style - see RangerTrackingUiState.mapStyleUri.
// Satellite tiles are much larger and out of scope for v1 offline caching; a ranger who
// switches to satellite view while offline falls back to normal online-tile behavior for
// that style only.
private const val OFFLINE_STYLE_URI = "mapbox://styles/mapbox/streets-v12"
private const val MIN_ZOOM: Byte = 0
private const val MAX_ZOOM: Byte = 16

interface MapOfflineRepository {
    /**
     * Downloads the base-map style pack + tile region covering [park]'s boundary so it
     * renders without a network connection. Safe to call repeatedly for the same park -
     * TileStore treats an already-cached region as a cheap verification, not a re-download.
     */
    suspend fun downloadParkRegion(park: NationalPark): Result<Unit>
}

@Singleton
class MapOfflineRepositoryImpl @Inject constructor() : MapOfflineRepository {

    // Mapbox's own recommendation is a single shared TileStore/OfflineManager instance for
    // the whole app; both are native-backed and only safe to touch after
    // MapboxOptions.accessToken is set (WildWatchApplication.onCreate), so they're lazy
    // rather than eagerly constructed at injection time.
    private val tileStore: TileStore by lazy { TileStore.create() }
    private val offlineManager: OfflineManager by lazy { OfflineManager() }

    override suspend fun downloadParkRegion(park: NationalPark): Result<Unit> = runCatching {
        val geometry = parseBoundaryGeometry(park.boundaryGeoJson)
            ?: error("Park ${park.id} has no usable boundary_geojson - cannot scope an offline tile region")

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(OFFLINE_STYLE_URI)
                .minZoom(MIN_ZOOM)
                .maxZoom(MAX_ZOOM)
                .build(),
        )

        loadStylePack()
        loadTileRegion(regionId = park.id, geometry = geometry, descriptor = tilesetDescriptor)
        Timber.d("Offline map region ready for park %s", park.id)
    }

    private suspend fun loadStylePack(): Unit = suspendCancellableCoroutine { continuation ->
        val cancelable = offlineManager.loadStylePack(
            OFFLINE_STYLE_URI,
            StylePackLoadOptions.Builder().build(),
        ) { expected ->
            if (expected.isValue) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    IOException(expected.error?.message ?: "Style pack download failed"),
                )
            }
        }
        continuation.invokeOnCancellation { cancelable.cancel() }
    }

    private suspend fun loadTileRegion(
        regionId: String,
        geometry: Geometry,
        descriptor: TilesetDescriptor,
    ): Unit = suspendCancellableCoroutine { continuation ->
        val options = TileRegionLoadOptions.Builder()
            .geometry(geometry)
            .descriptors(listOf(descriptor))
            .acceptExpired(true)
            // Automatic/silent trigger (no per-download user consent prompt) - keep this off
            // metered/cellular connections so it never silently burns a ranger's mobile data.
            .networkRestriction(NetworkRestriction.DISALLOW_EXPENSIVE)
            .build()
        val cancelable = tileStore.loadTileRegion(regionId, options) { expected ->
            if (expected.isValue) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    IOException(expected.error?.message ?: "Tile region download failed"),
                )
            }
        }
        continuation.invokeOnCancellation { cancelable.cancel() }
    }
}

// Top-level and internal (rather than a private method on the impl) so it's unit-testable
// without a real native TileStore/OfflineManager instance, which local JVM tests can't
// construct - see MapOfflineGeometryTest. Feature.fromJson is a plain Gson-backed parser
// (mapbox-sdk-geojson), not JNI, so this part alone is safe to exercise off-device.
internal fun parseBoundaryGeometry(boundaryGeoJson: String): Geometry? {
    if (boundaryGeoJson.isBlank()) return null
    return runCatching { Feature.fromJson(boundaryGeoJson).geometry() }.getOrNull()
}
