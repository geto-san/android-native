package com.wildwatch.app.core.di

import com.wildwatch.app.core.data.alert.AlertRepository
import com.wildwatch.app.core.data.alert.AlertRepositoryImpl
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.auth.AuthRepositoryImpl
import com.wildwatch.app.core.data.claim.ClaimRemoteDataSource
import com.wildwatch.app.core.data.claim.ClaimRemoteDataSourceImpl
import com.wildwatch.app.core.data.claim.ClaimRepository
import com.wildwatch.app.core.data.claim.ClaimRepositoryImpl
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.connectivity.ConnectivityObserverImpl
import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.data.feed.ArticleRepositoryImpl
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.location.LocationRepositoryImpl
import com.wildwatch.app.core.data.incident.IncidentRemoteDataSource
import com.wildwatch.app.core.data.incident.IncidentRemoteDataSourceImpl
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.IncidentRepositoryImpl
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

    // TEMPORARY: bound to the offline, DataStore-backed implementation while
    // this environment has no internet access to reach Firebase Auth. Revert
    // to `impl: AuthRepositoryImpl` (see its own class, still intact and
    // unused below) once connectivity is back - see OfflineAuthRepositoryImpl's
    // doc comment for the full revert note.
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
