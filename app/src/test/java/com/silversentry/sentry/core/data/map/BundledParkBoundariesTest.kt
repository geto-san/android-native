package com.silversentry.sentry.core.data.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledParkBoundariesTest {

    private val validFeatureCollection = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [[[29.9, -0.9], [30.1, -0.9], [30.1, -0.8], [29.9, -0.8], [29.9, -0.9]]]
              },
              "properties": { "id": "bwindi-impenetrable" }
            }
          ]
        }
    """.trimIndent()

    // Mirrors the assets/parks.geojson produced by scripts/generate_parks_geojson.mjs:
    // the exact FeatureCollection shape BundledParkBoundaries reads at runtime.
    private val generatedAsset = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [[[29.6, -1.1], [29.8, -1.1], [29.8, -1.0], [29.6, -1.0], [29.6, -1.1]]]
              },
              "properties": { "id": "bwindi-impenetrable" }
            },
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [[[29.55, -1.42], [29.75, -1.42], [29.75, -1.32], [29.55, -1.32], [29.55, -1.42]]]
              },
              "properties": { "id": "mgahinga-gorilla" }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parseBundledParks maps ids to their polygon rings`() {
        val parks = parseBundledParks(validFeatureCollection)

        assertEquals(1, parks.size)
        val ring = parks.getValue("bwindi-impenetrable")
        assertEquals(1, ring.size)
        assertEquals(5, ring[0].size)
        assertEquals(29.9, ring[0][0].longitude(), 0.0001)
        assertEquals(-0.9, ring[0][0].latitude(), 0.0001)
    }

    @Test
    fun `parseBundledParks handles the generated multi-park asset`() {
        val parks = parseBundledParks(generatedAsset)

        assertEquals(2, parks.size)
        assertTrue(parks.containsKey("bwindi-impenetrable"))
        assertTrue(parks.containsKey("mgahinga-gorilla"))
    }

    @Test
    fun `parseBundledParks flattens multipolygon features`() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "MultiPolygon",
                    "coordinates": [
                      [[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 0.0]]],
                      [[[2.0, 2.0], [3.0, 2.0], [3.0, 3.0], [2.0, 2.0]]]
                    ]
                  },
                  "properties": { "id": "split-park" }
                }
              ]
            }
        """.trimIndent()

        val parks = parseBundledParks(json)

        assertEquals(1, parks.size)
        assertEquals(2, parks.getValue("split-park").size)
    }

    @Test
    fun `parseBundledParks returns empty for blank input`() {
        assertTrue(parseBundledParks("").isEmpty())
        assertTrue(parseBundledParks("   ").isEmpty())
    }

    @Test
    fun `parseBundledParks returns empty for malformed JSON instead of throwing`() {
        assertTrue(parseBundledParks("{not valid geojson").isEmpty())
    }

    @Test
    fun `parseBundledParks drops features without an id or without polygon geometry`() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                { "type": "Feature", "geometry": { "type": "Point", "coordinates": [30.0, -1.0] }, "properties": { "id": "point-only" } },
                { "type": "Feature", "geometry": { "type": "Polygon", "coordinates": [[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 0.0]]] }, "properties": {} }
              ]
            }
        """.trimIndent()

        assertTrue(parseBundledParks(json).isEmpty())
    }
}