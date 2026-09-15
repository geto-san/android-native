package com.silversentry.sentry.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

// Ported from mihon's UserAgentInterceptor (core/common/.../network/interceptor):
// every outbound request gets a stable, identifying User-Agent unless one was
// already set, so the Laravel API can attribute the traffic this app generates
// without anything having to be set per-call on the Retrofit interface.
class UserAgentInterceptor(
    private val userAgentProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", userAgentProvider())
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
