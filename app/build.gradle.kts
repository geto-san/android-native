import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9+ has built-in Kotlin support; the separate org.jetbrains.kotlin.android
    // plugin is no longer applied (AGP rejects it if present).
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.wildwatch.app"
    // Several current AndroidX/Compose/Maps releases require compileSdk 37 to build
    // against, even though targetSdk (the runtime behavior opt-in) stays at 36.
    compileSdk = 37

    defaultConfig {
        // Distinct from the sibling Expo app's applicationId (com.silverback.sentry,
        // see app.json at the repo root) - this native app has its own Firebase
        // Android app registration under the same silverback-sentry-c6727 project.
        applicationId = "com.wildwatch.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Swaps in HiltTestApplication for @HiltAndroidTest instrumented tests.
        testInstrumentationRunner = "com.wildwatch.app.CustomTestRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val mapboxToken = localProperties.getProperty("PUBLIC_MAPBOX_ACCESS_TOKEN") ?: ""
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")

        val useLocalBackend = localProperties.getProperty("USE_LOCAL_BACKEND")?.toBoolean() ?: false
        buildConfigField("Boolean", "USE_LOCAL_BACKEND", "$useLocalBackend")

        val localBackendHost = localProperties.getProperty("LOCAL_BACKEND_HOST") ?: "10.0.2.2"
        buildConfigField("String", "LOCAL_BACKEND_HOST", "\"$localBackendHost\"")

        // Laravel API base URL for the mobile-direct bridge calls (Spark plan: no Cloud
        // Functions relay, so this app calls Laravel itself after writing to Firestore -
        // see IncidentRepositoryImpl.syncPending() and BRIDGE-CONTRACT.md). Mirrors
        // USE_LOCAL_BACKEND/LOCAL_BACKEND_HOST above when pointed at a local Laravel
        // instance; otherwise reads the deployed Render URL from local.properties. The
        // .invalid fallback (RFC 2606 reserved, resolves to nothing) fails loudly with a
        // network error instead of silently pointing at an unintended real host.
        val laravelApiBaseUrl =
            if (useLocalBackend) {
                "http://$localBackendHost:8000/api/"
            } else {
                localProperties.getProperty("LARAVEL_API_BASE_URL") ?: "https://unset.invalid/api/"
            }
        buildConfigField("String", "LARAVEL_API_BASE_URL", "\"$laravelApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Distributable QA/test build: same shrinking as release (the debug build type
        // never enables isMinifyEnabled/isShrinkResources, which is most of why a debug
        // APK runs ~100MB+ of unshrunk dex for this dependency set), but signed with the
        // built-in debug key since there's no real release keystore yet - installs
        // directly via adb/sideload without one. Not for production distribution; swap to
        // a real signing config once one exists. Build with `./gradlew assembleStaging`.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // Splits by ABI rather than shipping one APK bundling all four (arm64-v8a,
    // armeabi-v7a, x86, x86_64) copies of Mapbox's and SQLCipher's native libraries -
    // most of a test device's install only ever needs one. isUniversalApk stays false
    // since sideload-testing targets a real device's specific ABI, not an emulator
    // needing every architecture in one file.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Room's exported schema history (for future migration tests) - see AppDatabase.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

ktlint {
    version.set("1.5.0")
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.room.testing)

    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.work.testing)

    implementation(libs.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    // staging is a debug-signed sideload/QA build, not a Play-distributed release - Play
    // Integrity attestation would fail for it the same way it would for a debug build, so
    // it needs the same Debug App Check provider (see FirebaseModule).
    "stagingImplementation"(libs.firebase.appcheck.debug)

    implementation(libs.play.services.location)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.compose)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.sqlcipher)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.okhttp)

    implementation(libs.timber)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
