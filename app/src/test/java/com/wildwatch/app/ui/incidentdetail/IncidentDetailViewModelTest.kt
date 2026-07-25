package com.wildwatch.app.ui.incidentdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.data.location.GeoLocation
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.domain.model.Incident
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        locationRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun incident(id: String) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = IncidentStatus.OPEN,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = Severity.MEDIUM,
        summary = "Calm herd",
        lat = -1.0,
        lng = 29.0,
        locationName = "Buhoma sector",
        reportedAt = "2026-07-22T00:00:00Z",
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    private fun viewModelFor(id: String): IncidentDetailViewModel =
        IncidentDetailViewModel(SavedStateHandle(mapOf("id" to id)), incidentRepository, locationRepository)

    @Test
    fun `resolves the incident matching the route id out of the full list`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(listOf(incident("a"), incident("b")))

        val viewModel = viewModelFor("b")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("b", state.incident?.id)
            assertEquals(false, state.isLoading)
        }
    }

    @Test
    fun `distance is null until loadDistance resolves a location`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(listOf(incident("a")))

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertNull(awaitItem().distanceKm)
        }
    }

    @Test
    fun `loadDistance computes a haversine distance from the current location`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(listOf(incident("a")))
        // Roughly 1 degree of latitude south of the incident's (-1.0, 29.0).
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(-2.0, 29.0, 10f))

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertNull(awaitItem().distanceKm)
            viewModel.loadDistance()
            val distance = awaitItem().distanceKm
            assertEquals(111.0, distance!!, 2.0)
        }
    }
}
