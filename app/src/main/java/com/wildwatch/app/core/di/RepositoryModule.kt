package com.wildwatch.app.core.di

import com.wildwatch.app.core.data.alert.AlertRepository
import com.wildwatch.app.core.data.alert.AlertRepositoryImpl
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.auth.AuthRepositoryImpl
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.connectivity.ConnectivityObserverImpl
import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.data.feed.ArticleRepositoryImpl
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.location.LocationRepositoryImpl
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.data.notification.NotificationRepositoryImpl
import com.wildwatch.app.core.data.repository.LocationHierarchyRepository
import com.wildwatch.app.core.data.repository.LocationHierarchyRepositoryImpl
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.data.repository.ParkRepositoryImpl
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.data.user.UserDataRepositoryImpl
import com.wildwatch.app.core.data.incident.IncidentRemoteDataSource
import com.wildwatch.app.core.data.incident.IncidentRemoteDataSourceImpl
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.IncidentRepositoryImpl
import com.wildwatch.app.feature.dashboard.CommunityAlertPreferences
import com.wildwatch.app.feature.dashboard.CommunityAlertPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    abstract fun bindUserDataRepository(impl: UserDataRepositoryImpl): UserDataRepository

    @Binds
    abstract fun bindParkRepository(impl: ParkRepositoryImpl): ParkRepository

    @Binds
    abstract fun bindLocationHierarchyRepository(impl: LocationHierarchyRepositoryImpl): LocationHierarchyRepository

    @Binds
    abstract fun bindCommunityAlertPreferences(impl: CommunityAlertPreferencesImpl): CommunityAlertPreferences
}
