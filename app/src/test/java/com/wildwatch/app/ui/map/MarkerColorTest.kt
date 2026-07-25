package com.wildwatch.app.ui.map

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.wildwatch.app.data.local.db.Severity
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerColorTest {

    @Test
    fun `critical severity uses the red hue`() {
        assertEquals(BitmapDescriptorFactory.HUE_RED, markerHueFor(Severity.CRITICAL))
    }

    @Test
    fun `high severity uses the orange hue`() {
        assertEquals(BitmapDescriptorFactory.HUE_ORANGE, markerHueFor(Severity.HIGH))
    }

    @Test
    fun `medium severity uses the yellow hue`() {
        assertEquals(BitmapDescriptorFactory.HUE_YELLOW, markerHueFor(Severity.MEDIUM))
    }

    @Test
    fun `low severity uses the green hue`() {
        assertEquals(BitmapDescriptorFactory.HUE_GREEN, markerHueFor(Severity.LOW))
    }
}
