package com.silverback.sentry.data.connectivity

import kotlinx.coroutines.flow.Flow

// Per guardrail G7, this is the only connectivity surface the UI layer depends
// on - no screen touches ConnectivityManager directly.
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
