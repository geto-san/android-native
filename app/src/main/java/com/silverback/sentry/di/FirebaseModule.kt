package com.silverback.sentry.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Per guardrail G1/G7 (see the migration plan): these are the ONLY Firebase SDK
// instances the app constructs. They get injected into the data-layer repository
// implementations only (AuthRepositoryImpl, ObservationRepositoryImpl, etc.) -
// never directly into a ViewModel or Composable. Additional Firebase products
// (Messaging, AI) get their own @Provides here when their phase adds them,
// rather than speculatively wiring unused SDKs now.
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun providesFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}
