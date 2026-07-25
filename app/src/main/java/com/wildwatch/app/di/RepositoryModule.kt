package com.wildwatch.app.di

import com.wildwatch.app.data.alert.AlertRepository
import com.wildwatch.app.data.alert.AlertRepositoryImpl
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.auth.AuthRepositoryImpl
import com.wildwatch.app.data.claim.ClaimRemoteDataSource
import com.wildwatch.app.data.claim.ClaimRemoteDataSourceImpl
import com.wildwatch.app.data.claim.ClaimRepository
import com.wildwatch.app.data.claim.ClaimRepositoryImpl
import com.wildwatch.app.data.connectivity.ConnectivityObserver
import com.wildwatch.app.data.connectivity.ConnectivityObserverImpl
import com.wildwatch.app.data.feed.ArticleRepository
import com.wildwatch.app.data.feed.ArticleRepositoryImpl
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.data.location.LocationRepositoryImpl
import com.wildwatch.app.data.incident.IncidentRemoteDataSource
import com.wildwatch.app.data.incident.IncidentRemoteDataSourceImpl
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.incident.IncidentRepositoryImpl
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

    // Live Firebase-backed implementation. OfflineAuthRepositoryImpl (DataStore-based)
    // remains in the codebase as a fallback for offline/no-connectivity dev sandboxes -
    // swap the binding below back to it if testing without network access.
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindIncidentRepository(impl: IncidentRepositoryImpl): IncidentRepository

    @Binds
    abstract fun bindIncidentRemoteDataSource(impl: IncidentRemoteDataSourceImpl): IncidentRemoteDataSource

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    abstract fun bindConnectivityObserver(impl: ConnectivityObserverImpl): ConnectivityObserver

    @Binds
    abstract fun bindClaimRepository(impl: ClaimRepositoryImpl): ClaimRepository

    @Binds
    abstract fun bindClaimRemoteDataSource(impl: ClaimRemoteDataSourceImpl): ClaimRemoteDataSource

    @Binds
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository
}
