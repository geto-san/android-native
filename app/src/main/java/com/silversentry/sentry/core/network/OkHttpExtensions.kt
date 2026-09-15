package com.silversentry.sentry.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

// Ported from mihon's OkHttpExtensions (core/common/.../network/OkHttpExtensions.kt):
// suspend wrappers that adapt OkHttp's callback-based API to a cancellable coroutine.
// Cancelling the caller's coroutine cancels the underlying call (fetching a cancelled
// request leaves the server to the bridge timeout instead of hanging the UI), and
// non-2xx responses surface as an IOException carrying the HTTP code - the same shape
// Retrofit's suspend bridges already throw, so non-Retrofit OkHttp call sites behave
// identically to the rest of the network layer.
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }

    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

suspend fun Call.awaitSuccess(): Response {
    val response = await()
    if (!response.isSuccessful) {
        response.close()
        throw IOException("HTTP ${response.code}")
    }
    return response
}
