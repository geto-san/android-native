package com.wildwatch.app.core.model

import com.mapbox.geojson.Point

data class NationalPark(
    val id: String = "",
    val name: String = "",
    val center: Point = Point.fromLngLat(0.0, 0.0),
    val zoomLevel: Double = 12.0,
    val boundaryPoints: List<Point> = emptyList()
)

enum class AttractionType {
    ANIMAL_HABITAT,
    LANDMARK,
    RANGER_STATION,
    GATE,
    WATER_SOURCE,
    VIEWPOINT
}

data class ParkAttraction(
    val id: String = "",
    val parkId: String = "",
    val name: String = "",
    val type: AttractionType = AttractionType.LANDMARK,
    val location: Point = Point.fromLngLat(0.0, 0.0),
    val description: String = "",
    val animalSpecies: String? = null // Only for ANIMAL_HABITAT type
)
