package com.wildwatch.app.core.data.map

import com.mapbox.geojson.Polygon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOfflineRepositoryTest {

    // Mirrors android-native-backend-branch/scripts/seed.ts's seedParks() shape exactly:
    // a Feature wrapping a Polygon boundary.
    private val validBoundaryGeoJson = """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [[[29.9, -0.9], [30.1, -0.9], [30.1, -0.8], [29.9, -0.8], [29.9, -0.9]]]
          }
        }
    """.trimIndent()

    @Test
    fun `parseBoundaryGeometry parses a seeded Feature-wrapped Polygon`() {
        val geometry = parseBoundaryGeometry(validBoundaryGeoJson)

        assertTrue(geometry is Polygon)
        val polygon = geometry as Polygon
        assertEquals(1, polygon.coordinates().size)
        assertEquals(5, polygon.coordinates()[0].size)
    }

    @Test
    fun `parseBoundaryGeometry returns null for a blank string`() {
        assertNull(parseBoundaryGeometry(""))
        assertNull(parseBoundaryGeometry("   "))
    }

    @Test
    fun `parseBoundaryGeometry returns null for malformed JSON instead of throwing`() {
        assertNull(parseBoundaryGeometry("{not valid geojson"))
    }

    @Test
    fun `parseBoundaryGeometry returns null for valid JSON with no geometry`() {
        assertNull(parseBoundaryGeometry("""{"type": "Feature", "properties": {}}"""))
    }
}
