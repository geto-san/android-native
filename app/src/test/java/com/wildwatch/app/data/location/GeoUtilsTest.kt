package com.wildwatch.app.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `distance between identical points is zero`() {
        assertEquals(0.0, haversineKm(-1.5, 29.5, -1.5, 29.5), 0.0001)
    }

    @Test
    fun `one degree of latitude is roughly 111km`() {
        assertEquals(111.0, haversineKm(0.0, 0.0, 1.0, 0.0), 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val ab = haversineKm(-1.0, 29.0, -1.5, 29.5)
        val ba = haversineKm(-1.5, 29.5, -1.0, 29.0)
        assertEquals(ab, ba, 0.0001)
    }
}
