package com.wildwatch.app.core.di

import com.wildwatch.app.BuildConfig
import com.wildwatch.app.core.data.bridge.LaravelBridgeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

// Provides the plain HTTP client used for the mobile-direct Laravel bridge calls (see
// LaravelBridgeDataSource) - distinct from FirebaseModule, which only provides Firebase SDK
// instances.
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.LARAVEL_API_BASE_URL)
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    fun providesLaravelBridgeApi(retrofit: Retrofit): LaravelBridgeApi =
        retrofit.create(LaravelBridgeApi::class.java)
}
