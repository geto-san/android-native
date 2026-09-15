package com.silversentry.sentry.core.network.interceptor

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

// Exercises RetryingInterceptor purely against a canned fake Chain (no socket, no threads):
// the interceptor's only job is deciding which responses to retry and when to stop, so a
// scripted sequence of HTTP codes is exactly the surface it needs to be pinned down against.
class RetryingInterceptorTest {

    private val interceptor = RetryingInterceptor(maxAttempts = 3, baseDelayMs = 0)

    @Test
    fun `returns immediately on a 2xx`() {
        val chain = FakeChain(200, 500, 500, 500)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.attempts)
    }

    @Test
    fun `retries a transient 503 and succeeds on the second attempt`() {
        val chain = FakeChain(503, 200)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.attempts)
    }

    @Test
    fun `gives up after maxAttempts on a persistent 503`() {
        val chain = FakeChain(503, 503, 503, 503)

        val response = interceptor.intercept(chain)

        assertEquals(503, response.code)
        assertEquals(3, chain.attempts)
    }

    @Test
    fun `does not retry a 4xx client error`() {
        val chain = FakeChain(400, 200)

        assertEquals(400, interceptor.intercept(chain).code)
        assertEquals(1, chain.attempts)
    }

    @Test
    fun `does not retry a non-list 5xx code such as 501`() {
        val chain = FakeChain(501, 200)

        assertEquals(501, interceptor.intercept(chain).code)
        assertEquals(1, chain.attempts)
    }

    private inner class FakeChain(vararg statuses: Int) : Interceptor.Chain {
        private val responses = ArrayDeque(statuses.map { fakeResponse(it) })
        var attempts = 0
            private set

        override fun request(): Request = fakeRequest()

        override fun proceed(request: Request): Response {
            attempts++
            return responses.removeFirst()
        }

        override fun connection(): Connection? = null

        override fun call(): Call = throw UnsupportedOperationException("No real call in a fake chain")

        override fun connectTimeoutMillis(): Int = 0

        override fun readTimeoutMillis(): Int = 0

        override fun writeTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private fun fakeResponse(code: Int): Response = Response.Builder()
        .request(fakeRequest())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("status $code")
        .body("".toResponseBody(null))
        .build()

    private fun fakeRequest(): Request = Request.Builder().url("https://example.com/mobile/incidents").build()
}
