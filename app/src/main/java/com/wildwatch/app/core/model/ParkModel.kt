package com.wildwatch.app.core.model

import com.google.firebase.firestore.Exclude
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
    // Derived, not a stored field - excluded so Firestore's POJO mapper
    // doesn't try (and fail) to serialize a Mapbox Point, which has no
    // bean-style properties of its own. Only matters once something writes a
    // NationalPark back to Firestore; harmless if that never happens, but
    // cheap insurance against the same crash createAttraction() below hit.
    @get:Exclude
    val center: Point get() = location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(0.0, 0.0)
}

enum class AttractionType {
    ANIMAL_HABITAT,
    LANDMARK,
    RANGER_STATION,
    GATE,
    WATER_SOURCE,
    VIEWPOINT,
    DANGER_ZONE
}

data class ParkAttraction(
    var id: String = "",
    var parkId: String = "",
    var name: String = "",
    var type: AttractionType = AttractionType.LANDMARK,
    @get:PropertyName("location") @set:PropertyName("location") @field:PropertyName("location")
    var location: GeoPoint? = null,
    var description: String = "",
    var animalSpecies: String? = null, // Only for ANIMAL_HABITAT type
    // Set only for points added in-app (e.g. a ranger flagging a danger zone
    // on discovery) - admin/UWA-seeded attractions leave these null.
    var reportedBy: String? = null,
    var createdAt: String? = null,
) {
    // Same reasoning as NationalPark.center - derived, not a stored field,
    // and this one is no longer hypothetical: ParkRepositoryImpl.createAttraction()
    // writes this POJO with .set(), and Firestore's reflection-based mapper
    // serializes every public getter unless told not to - it doesn't know
    // how to serialize a raw com.mapbox.geojson.Point and throws
    // "No properties to serialize found on class com.mapbox.geojson.Point".
    @get:Exclude
    val point: Point get() = location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(0.0, 0.0)
}
