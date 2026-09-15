package com.silversentry.sentry.core.sync

import com.silversentry.sentry.core.data.connectivity.ConnectivityObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OutboxSyncListenerTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun listener(
        online: MutableStateFlow<Boolean>,
        scheduler: SyncScheduler,
    ) = OutboxSyncListener(
        connectivityObserver = mockk<ConnectivityObserver>().also { every { it.isOnline } returns online },
        syncScheduler = scheduler,
        applicationScope = CoroutineScope(testDispatcher),
    )

    @Test
    fun `triggers immediate sync when connectivity is observed again`() = runTest(testDispatcher) {
        val online = MutableStateFlow(true)
        val scheduler = mockk<SyncScheduler>()
        coEvery { scheduler.triggerImmediateSync() } answers {}

        val listener = listener(online, scheduler)
        listener.start()

        // Initial (already-online) emission is intentionally skipped - it's not an edge.
        runCurrent()
        coVerify(exactly = 0) { scheduler.triggerImmediateSync() }

        online.value = false
        runCurrent()
        online.value = true
        advanceUntilIdle()

        coVerify(exactly = 1) { scheduler.triggerImmediateSync() }
    }

    @Test
    fun `does not sync on the initial emission`() = runTest(testDispatcher) {
        val online = MutableStateFlow(true)
        val scheduler = mockk<SyncScheduler>()
        coEvery { scheduler.triggerImmediateSync() } answers {}

        listener(online, scheduler).start()
        runCurrent()

        coVerify(exactly = 0) { scheduler.triggerImmediateSync() }
    }

    @Test
    fun `start is idempotent so the connectivity edge only ever syncs once`() = runTest(testDispatcher) {
        val online = MutableStateFlow(true)
        val scheduler = mockk<SyncScheduler>()
        coEvery { scheduler.triggerImmediateSync() } answers {}

        val listener = listener(online, scheduler)
        listener.start()
        listener.start()
        runCurrent()

        online.value = false
        runCurrent()
        online.value = true
        advanceUntilIdle()

        coVerify(exactly = 1) { scheduler.triggerImmediateSync() }
    }
}
