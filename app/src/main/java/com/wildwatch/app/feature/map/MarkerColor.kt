package com.wildwatch.app.feature.map

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.wildwatch.app.core.database.Severity

fun markerHueFor(severity: Severity): Float = when (severity) {
    Severity.LOW -> BitmapDescriptorFactory.HUE_GREEN
    Severity.MEDIUM -> BitmapDescriptorFactory.HUE_YELLOW
    Severity.HIGH -> BitmapDescriptorFactory.HUE_ORANGE
    Severity.CRITICAL -> BitmapDescriptorFactory.HUE_RED
}
