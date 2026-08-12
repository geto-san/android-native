package com.wildwatch.app.core.sync

import com.wildwatch.app.core.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapCoordinatorTest {

    @Test
    fun `shouldPrefetchOfflineMap is true for a ranger with an assigned park`() {
        assertTrue(shouldPrefetchOfflineMap(UserRole.RANGER, "park-1"))
    }

    @Test
    fun `shouldPrefetchOfflineMap is false when parkId is null`() {
        assertFalse(shouldPrefetchOfflineMap(UserRole.RANGER, null))
    }

    @Test
    fun `shouldPrefetchOfflineMap is false when parkId is blank`() {
        assertFalse(shouldPrefetchOfflineMap(UserRole.RANGER, ""))
        assertFalse(shouldPrefetchOfflineMap(UserRole.RANGER, "   "))
    }

    @Test
    fun `shouldPrefetchOfflineMap is false for non-ranger roles even with a park assigned`() {
        assertFalse(shouldPrefetchOfflineMap(UserRole.WARDEN, "park-1"))
        assertFalse(shouldPrefetchOfflineMap(UserRole.UWA_OFFICIAL, "park-1"))
        assertFalse(shouldPrefetchOfflineMap(UserRole.PUBLIC, "park-1"))
    }
}
