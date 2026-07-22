// Top-level build file: plugin versions declared here (applied per-module with `apply false`)
// so every module in the project resolves the same plugin version.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9+ has built-in Kotlin support; org.jetbrains.kotlin.android is no longer used.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
