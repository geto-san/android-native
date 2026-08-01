package com.wildwatch.app.core.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.mapbox.geojson.Point

data class NationalPark(
    var id: String = "",
    var name: String = "",
    @get:PropertyName("location") @set:PropertyName("location") @field:PropertyName("location")
    var location: GeoPoint? = null,
    var districts: List<String> = emptyList(),
    var description: String = "",
    var zoomLevel: Double = 12.0,
    @get:PropertyName("boundary_geojson") @set:PropertyName("boundary_geojson") @field:PropertyName("boundary_geojson")
    var boundaryGeoJson: String = ""
) {
    val center: Point get() = location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(0.0, 0.0)
}

enum class AttractionType {
    ANIMAL_HABITAT,
    LANDMARK,
    RANGER_STATION,
    GATE,
    WATER_SOURCE,
    VIEWPOINT
}

data class ParkAttraction(
    var id: String = "",
    var parkId: String = "",
    var name: String = "",
    var type: AttractionType = AttractionType.LANDMARK,
    @get:PropertyName("location") @set:PropertyName("location") @field:PropertyName("location")
    var location: GeoPoint? = null,
    var description: String = "",
    var animalSpecies: String? = null // Only for ANIMAL_HABITAT type
) {
    val point: Point get() = location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(0.0, 0.0)
}
