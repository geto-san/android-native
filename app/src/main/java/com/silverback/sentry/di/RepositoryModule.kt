package com.silverback.sentry.di

import com.silverback.sentry.data.auth.AuthRepository
import com.silverback.sentry.data.auth.AuthRepositoryImpl
import com.silverback.sentry.data.connectivity.ConnectivityObserver
import com.silverback.sentry.data.connectivity.ConnectivityObserverImpl
import com.silverback.sentry.data.location.LocationRepository
import com.silverback.sentry.data.location.LocationRepositoryImpl
import com.silverback.sentry.data.observation.ObservationRemoteDataSource
import com.silverback.sentry.data.observation.ObservationRemoteDataSourceImpl
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.data.observation.ObservationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// @Binds wiring from repository interfaces to their single implementation. Each
// phase that adds a repository (ChatRepository in Phase 10, ...) adds one @Binds
// method here rather than a new module per repository.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindObservationRepository(impl: ObservationRepositoryImpl): ObservationRepository

    @Binds
    abstract fun bindObservationRemoteDataSource(impl: ObservationRemoteDataSourceImpl): ObservationRemoteDataSource

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    abstract fun bindConnectivityObserver(impl: ConnectivityObserverImpl): ConnectivityObserver
}
