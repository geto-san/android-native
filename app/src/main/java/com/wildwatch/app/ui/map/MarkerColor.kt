package com.wildwatch.app.ui.map

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.wildwatch.app.data.local.db.Severity

// Pure severity -> marker hue mapping (BitmapDescriptorFactory's HUE_* constants
// are plain Float values, not tied to a live Map instance, so this is directly
// unit-testable without an emulator). Severity, not status - a CRITICAL incident
// should stand out on the map regardless of whether a ranger has claimed it yet.
fun markerHueFor(severity: Severity): Float = when (severity) {
    Severity.CRITICAL -> BitmapDescriptorFactory.HUE_RED
    Severity.HIGH -> BitmapDescriptorFactory.HUE_ORANGE
    Severity.MEDIUM -> BitmapDescriptorFactory.HUE_YELLOW
    Severity.LOW -> BitmapDescriptorFactory.HUE_GREEN
}
