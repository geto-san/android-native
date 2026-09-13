package com.wildwatch.app.feature.tracking

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PolygonAnnotation

// Park boundary outline layered underneath every other tourism annotation. The rings come
// from RangerTrackingUiState.parkBoundaryRings (Firestore boundary_geojson when available,
// BundledParkBoundaries fallback offline). A single PolygonAnnotation carries every ring,
// with a soft translucent fill so the outline stays identifiable without hiding the patrol
// route or attraction markers stacked on top.
@Composable
@MapboxMapComposable
internal fun ParkBoundaryOverlay(polygons: List<List<Point>>) {
    val boundaryRings = polygons.filter { it.size >= 4 }
    if (boundaryRings.isEmpty()) return
    PolygonAnnotation(points = boundaryRings) {
        fillColor = Color(0xFF2E7D32)
        fillOpacity = 0.25
        fillOutlineColor = Color(0xFF2E7D32)
    }
}