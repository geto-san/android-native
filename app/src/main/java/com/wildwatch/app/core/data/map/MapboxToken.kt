package com.wildwatch.app.core.data.map

import com.wildwatch.app.BuildConfig

/**
 * True only if `local.properties`'s PUBLIC_MAPBOX_ACCESS_TOKEN was actually populated at build
 * time. Mapbox's SDK throws immediately when a MapView initializes with a blank/placeholder
 * token, so every entry point that renders a MapboxMap composable must check this first instead
 * of letting the SDK crash the screen.
 */
fun isMapboxTokenConfigured(): Boolean {
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN
    return token.isNotBlank() && !token.startsWith("YOUR_")
}
