package com.silversentry.sentry.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.HttpMethod
import timber.log.Timber

// Mirrors the hardened-OkHttp stance of mihon's network stack (generous
// timeouts + explicit retry handling instead of "one attempt and give up").
// The Laravel bridge upserts by a client-generated docId, so a bridge POST is
// idempotent: retrying it after a transient server error can never double-create
// a row. This interceptor transparently retries exactly those transient 5xx/429
// responses a couple of times with a short backoff, so a single overloaded
// response is not what leaves an incident stuck in the outbox.
//
// Transport-level flakiness (connect refused, socket reset, timeouts) is left to
// OkHttp's own retryOnConnectionFailure, which already replays the same buffered
// request body - this layer only handles the "the server answered but said 503"
// case that OkHttp's retry never sees.
class RetryingInterceptor(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
) : Interceptor {

    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(baseDelayMs >= 0) { "baseDelayMs must be >= 0" }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 1

        while (true) {
            val response = chain.proceed(request)
            if (!shouldRetry(response.code) || attempt >= maxAttempts) {
                return response
            }

            // Must not leak the connection for the response we are discarding.
            response.close()
            sleepBackoff(attempt)
            attempt++
        }
    }

    private fun shouldRetry(code: Int): Boolean = code in RETRYABLE_STATUS_CODES

    private fun sleepBackoff(attempt: Int) {
        val delayMs = baseDelayMs * (1L shl (attempt - 1))
        try {
            Thread.sleep(delayMs)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            Timber.w(interrupted, "Retry backoff interrupted after attempt %d", attempt)
        }
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_BASE_DELAY_MS = 500L

        // 429 Too Many Requests first, then the gateway-style 5xx codes that mean
        // "the request never got a real chance" (a transient overload, not a bug in
        // the payload). 4xx client errors are deliberately NOT retried - they are
        // permanent and re-sending them only wastes the same round trip.
        val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)
    }
}
