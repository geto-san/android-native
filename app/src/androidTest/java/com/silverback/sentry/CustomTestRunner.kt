package com.silverback.sentry

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Swaps in HiltTestApplication for instrumented tests so @HiltAndroidTest classes
// get a fresh, test-scoped Hilt component graph instead of the real SentryApplication.
// Wired in via app/build.gradle.kts's testInstrumentationRunner.
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
