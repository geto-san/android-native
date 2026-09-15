package com.silversentry.sentry.core.di

import com.silversentry.sentry.BuildConfig
import com.silversentry.sentry.core.data.bridge.LaravelBridgeApi
import com.silversentry.sentry.core.data.directions.DirectionsRepository
import com.silversentry.sentry.core.data.directions.DirectionsRepositoryImpl
import com.silversentry.sentry.core.network.interceptor.RetryingInterceptor
import com.silversentry.sentry.core.network.interceptor.UserAgentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Provides the plain HTTP client used for the mobile-direct Laravel bridge calls (see
// LaravelBridgeDataSource) - distinct from FirebaseModule, which only provides Firebase SDK
// instances.
//
// The client is configured the way mihon's NetworkHelper configures its own (generous
// connect/read/write timeouts, an overall call timeout, connection-pool reuse,
// retryOnConnectionFailure, a User-Agent interceptor, and debug-gated HTTP logging): a
// bare `OkHttpClient.Builder().build()` keeps OkHttp's 10-second default timeouts and
// `retryOnConnectionFailure = false`, which is exactly the fragility that let a single
// transient network blip fail a bridge POST and park the incident back in the outbox.
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val CALL_TIMEOUT_MINUTES = 2L

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 30, TimeUnit.SECONDS))
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            // Where the request body is replayable (the bridge's buffered JSON always is),
            // OkHttp transparently retries transport-level failures instead of failing the
            // call on the first socket hiccup.
            .retryOnConnectionFailure(true)
            .addInterceptor(
                UserAgentInterceptor { "SilverBackSentry/${BuildConfig.VERSION_NAME} (Android)" },
            )
            // Server-side transient responses (5xx/429) on the idempotent bridge POSTs are
            // retried here; see RetryingInterceptor for the rationale.
            .addInterceptor(RetryingInterceptor())

        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }

        return builder.build()
    }

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

    @Provides
    @Singleton
    fun providesDirectionsRepository(okHttpClient: OkHttpClient): DirectionsRepository =
        DirectionsRepositoryImpl(okHttpClient)
}
