package com.wildwatch.app.core.di

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Per guardrail G1/G7 (see the migration plan): these are the ONLY Firebase SDK
// instances the app constructs. They get injected into the data-layer repository
// implementations only (AuthRepositoryImpl, IncidentRepositoryImpl, etc.) -
// never directly into a ViewModel or Composable. Additional Firebase products
// (Messaging, AI) get their own @Provides here when their phase adds them,
// rather than speculatively wiring unused SDKs now.
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun providesFirebaseAppCheck(): FirebaseAppCheck {
        val appCheck = FirebaseAppCheck.getInstance()
        // Play Integrity is the recommended provider for Android, but it requires
        // Play-channel distribution/attestation to actually pass. Both debug and the
        // debug-signed "staging" QA build (see build.gradle.kts) are sideloaded, not
        // Play-distributed, so both use the Debug provider - checking BuildConfig.DEBUG
        // alone would miss staging and silently install a provider that always fails there.
        if (com.wildwatch.app.BuildConfig.BUILD_TYPE != "release") {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        return appCheck
    }

    @Provides
    @Singleton
    fun providesFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        if (com.wildwatch.app.BuildConfig.USE_LOCAL_BACKEND) {
            useEmulator(com.wildwatch.app.BuildConfig.LOCAL_BACKEND_HOST, 9099)
        }
    }

    @Provides
    @Singleton
    fun providesFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        if (com.wildwatch.app.BuildConfig.USE_LOCAL_BACKEND) {
            useEmulator(com.wildwatch.app.BuildConfig.LOCAL_BACKEND_HOST, 8080)
        }
    }

    @Provides
    @Singleton
    fun providesFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance().apply {
        if (com.wildwatch.app.BuildConfig.USE_LOCAL_BACKEND) {
            useEmulator(com.wildwatch.app.BuildConfig.LOCAL_BACKEND_HOST, 9199)
        }
    }

    @Provides
    @Singleton
    fun providesFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance().apply {
        if (com.wildwatch.app.BuildConfig.USE_LOCAL_BACKEND) {
            useEmulator(com.wildwatch.app.BuildConfig.LOCAL_BACKEND_HOST, 5001)
        }
    }
}
