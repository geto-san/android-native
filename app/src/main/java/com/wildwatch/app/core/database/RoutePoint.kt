package com.wildwatch.app.core.database

import kotlinx.serialization.Serializable

@Serializable
data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val timestamp: String,
)
