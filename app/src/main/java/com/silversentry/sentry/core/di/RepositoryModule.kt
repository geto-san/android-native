package com.silversentry.sentry.core.di

import com.silversentry.sentry.core.data.alert.AlertRepository
import com.silversentry.sentry.core.data.alert.AlertRepositoryImpl
import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.auth.AuthRepositoryImpl
import com.silversentry.sentry.core.data.bridge.LaravelBridgeDataSource
import com.silversentry.sentry.core.data.bridge.LaravelBridgeDataSourceImpl
import com.silversentry.sentry.core.data.connectivity.ConnectivityObserver
import com.silversentry.sentry.core.data.connectivity.ConnectivityObserverImpl
import com.silversentry.sentry.core.data.feed.ArticleRepository
import com.silversentry.sentry.core.data.feed.ArticleRepositoryImpl
import com.silversentry.sentry.core.data.location.LocationRepository
import com.silversentry.sentry.core.data.location.LocationRepositoryImpl
import com.silversentry.sentry.core.data.map.MapOfflineRepository
import com.silversentry.sentry.core.data.map.MapOfflineRepositoryImpl
import com.silversentry.sentry.core.data.notification.NotificationRepository
import com.silversentry.sentry.core.data.notification.NotificationRepositoryImpl
import com.silversentry.sentry.core.data.patrol.PatrolRemoteDataSource
import com.silversentry.sentry.core.data.patrol.PatrolRemoteDataSourceImpl
import com.silversentry.sentry.core.data.patrol.PatrolRepository
import com.silversentry.sentry.core.data.patrol.PatrolRepositoryImpl
import com.silversentry.sentry.core.data.repository.ParkRepository
import com.silversentry.sentry.core.data.repository.ParkRepositoryImpl
import com.silversentry.sentry.core.data.user.UserDataRepository
import com.silversentry.sentry.core.data.user.UserDataRepositoryImpl
import com.silversentry.sentry.core.data.incident.IncidentRemoteDataSource
import com.silversentry.sentry.core.data.incident.IncidentRemoteDataSourceImpl
import com.silversentry.sentry.core.data.incident.IncidentRepository
import com.silversentry.sentry.core.data.incident.IncidentRepositoryImpl
import com.silversentry.sentry.feature.dashboard.CommunityAlertPreferences
import com.silversentry.sentry.feature.dashboard.CommunityAlertPreferencesImpl
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
    abstract fun bindLaravelBridgeDataSource(impl: LaravelBridgeDataSourceImpl): LaravelBridgeDataSource

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
    abstract fun bindCommunityAlertPreferences(impl: CommunityAlertPreferencesImpl): CommunityAlertPreferences

    @Binds
    abstract fun bindPatrolRepository(impl: PatrolRepositoryImpl): PatrolRepository

    @Binds
    abstract fun bindPatrolRemoteDataSource(impl: PatrolRemoteDataSourceImpl): PatrolRemoteDataSource

    @Binds
    abstract fun bindMapOfflineRepository(impl: MapOfflineRepositoryImpl): MapOfflineRepository
}
