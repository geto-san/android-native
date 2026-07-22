package com.silverback.sentry.ui.home

import app.cash.turbine.test
import com.silverback.sentry.data.connectivity.ConnectivityObserver
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.domain.model.Observation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var observationRepository: ObservationRepository
    private lateinit var connectivityObserver: ConnectivityObserver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observationRepository = mockk()
        connectivityObserver = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun observation(
        id: String,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
        observationStatus: ObservationStatus = ObservationStatus.PENDING,
    ) = Observation(
        id = id,
        gorillaGroup = "Susa Group",
        location = "-1.5, 29.5",
        locationName = "Volcanoes National Park",
        healthStatus = "Healthy",
        notes = null,
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        createdAt = "2026-07-22T00:00:00Z",
        observationStatus = observationStatus,
        syncStatus = syncStatus,
        lastModified = 1000L,
    )

    @Test
    fun `counts are derived correctly from the observation list`() = runTest(testDispatcher) {
        every { observationRepository.observeAll() } returns MutableStateFlow(
            listOf(
                observation("a", syncStatus = SyncStatus.PENDING, observationStatus = ObservationStatus.PENDING),
                observation("b", syncStatus = SyncStatus.SYNCED, observationStatus = ObservationStatus.ATTENDED),
                observation("c", syncStatus = SyncStatus.SYNCED, observationStatus = ObservationStatus.PENDING),
            ),
        )
        every { connectivityObserver.isOnline } returns MutableStateFlow(true)

        val viewModel = HomeViewModel(observationRepository, connectivityObserver)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.totalCount)
            assertEquals(1, state.pendingSyncCount)
            assertEquals(1, state.attendedCount)
            assertEquals(2, state.needsAttentionCount)
            assertEquals(true, state.isOnline)
        }
    }

    @Test
    fun `an empty observation list produces all-zero counts`() = runTest(testDispatcher) {
        every { observationRepository.observeAll() } returns MutableStateFlow(emptyList())
        every { connectivityObserver.isOnline } returns MutableStateFlow(false)

        val viewModel = HomeViewModel(observationRepository, connectivityObserver)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalCount)
            assertEquals(0, state.pendingSyncCount)
            assertEquals(0, state.attendedCount)
            assertEquals(0, state.needsAttentionCount)
            assertEquals(false, state.isOnline)
        }
    }

    @Test
    fun `counts update live as the underlying flow emits a new list`() = runTest(testDispatcher) {
        val observationsFlow = MutableStateFlow(listOf(observation("a", syncStatus = SyncStatus.PENDING)))
        every { observationRepository.observeAll() } returns observationsFlow
        every { connectivityObserver.isOnline } returns MutableStateFlow(true)

        val viewModel = HomeViewModel(observationRepository, connectivityObserver)

        viewModel.uiState.test {
            assertEquals(1, awaitItem().totalCount)
            observationsFlow.value = listOf(
                observation("a", syncStatus = SyncStatus.SYNCED),
                observation("b", syncStatus = SyncStatus.PENDING),
            )
            val updated = awaitItem()
            assertEquals(2, updated.totalCount)
            assertEquals(1, updated.pendingSyncCount)
        }
    }
}
