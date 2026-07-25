package com.wildwatch.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Placeholder confirming the instrumented test runner (connectedDebugAndroidTest) is
// wired and can resolve the app's applicationId. Room DAO / Compose UI instrumented
// tests land in Phase 3 onward.
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.wildwatch.app", appContext.packageName)
    }
}
