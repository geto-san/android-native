package com.silversentry.sentry.core.sync

import com.silversentry.sentry.core.data.connectivity.ConnectivityObserver
import com.silversentry.sentry.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Closes the "incident got stuck in the outbox forever while sitting online" gap that pure
// WorkManager scheduling left behind:
//   * The periodic job only runs on WorkManager's schedule (15 minutes) plus the OS's own
//     reconnect nudge - fine as a backstop, but a freshly-created incident could still sit
//     quiet for a quarter-hour even on a healthy connection.
//   * The immediate one-shot is only enqueued by specific UI flows, so offline submissions
//     (the user had no network when they saved) never get an immediate trigger once the
//     device comes back online.
// This listener watches the single ConnectivityObserver surface (guardrail G7) and fires the
// immediate sync the moment connectivity is observed again, so rows that were queued while
// offline drain as soon as the network returns. WorkManager's own NetworkType.CONNECTED
// constraint makes the enqueued request a no-op anyway if connectivity flaps again before
// the worker runs, so there is no double-sync risk from also nudging it here.
@Singleton
class OutboxSyncListener @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private var started = false
    private var collectionJob: Job? = null

    fun start() {
        if (started) return
        started = true

        // Skip the initial (already-current) state: start() runs at app launch, and calling
        // triggerImmediateSync() on the very first emission would just duplicate the periodic
        // job's first run for no value. We only act on edges - offline -> online.
        collectionJob = applicationScope.launch {
            connectivityObserver.isOnline
                .drop(1)
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline) {
                        syncScheduler.triggerImmediateSync()
                    }
                }
        }
    }
}
