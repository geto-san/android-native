package com.wildwatch.app.feature.tracking

import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.wildwatch.app.R
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.model.AttractionType

// Distinct color-coded pins by incident type so rangers can tell severity/kind
// apart at a glance on the map instead of Mapbox's default unstyled red pin.
@Composable
internal fun rememberIncidentMarkerIcon(type: IncidentType): IconImage =
    rememberIconImage(
        when (type) {
            IncidentType.SIGHTING -> R.drawable.ic_marker_sighting
            IncidentType.CONFLICT -> R.drawable.ic_marker_conflict
            IncidentType.EMERGENCY -> R.drawable.ic_marker_emergency
            IncidentType.POACHING -> R.drawable.ic_marker_poaching
            IncidentType.SNARE -> R.drawable.ic_marker_snare
        },
    )

// Marker source drawables are 48dp rendered at 1x density (Mapbox's
// IconImage rasterizer ignores screen density), so these are relative scale
// factors off that 48px baseline, not absolute sizes - HIGH severity reads as
// visibly larger than LOW/LIGHT at any zoom level.
internal fun incidentMarkerIconSize(severity: IncidentSeverity): Double = when (severity) {
    IncidentSeverity.HIGH -> 0.7
    IncidentSeverity.MEDIUM -> 0.55
    IncidentSeverity.LOW -> 0.45
    IncidentSeverity.LIGHT -> 0.4
}

@Composable
internal fun rememberAttractionMarkerIcon(type: AttractionType): IconImage =
    // Danger zones get their own hazard-triangle marker since they're
    // safety-relevant; every other attraction type shares one neutral pin -
    // distinguishing POIs from incidents at a glance was the goal there, not
    // per-type icons.
    rememberIconImage(
        if (type == AttractionType.DANGER_ZONE) R.drawable.ic_marker_danger_zone else R.drawable.ic_marker_poi,
    )

internal const val ATTRACTION_MARKER_ICON_SIZE = 0.6
