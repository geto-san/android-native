package com.silverback.sentry.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// Qualified dispatchers so repositories/use cases inject an interface rather than
// calling Dispatchers.IO directly - tests substitute a TestDispatcher through the
// same qualifier without touching production code (see AGENTS testing strategy:
// this is what makes ObservationRepository's coroutine code unit-testable with
// kotlinx-coroutines-test in Phase 4).

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

// Long-lived scope for singleton repositories that expose a StateFlow derived from
// a callback-based SDK API (FirebaseAuth's AuthStateListener, Firestore snapshot
// listeners, NetworkCallback, ...) via stateIn(). Not for one-off async work -
// ViewModels use viewModelScope, not this.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @ApplicationScope
    @Provides
    @Singleton
    fun providesApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
}
