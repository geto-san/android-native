package com.wildwatch.app.core.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentByIdUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.feature.incidentdetail.IncidentDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var getIncidentByIdUseCase: GetIncidentByIdUseCase
    private lateinit var observeUserUseCase: ObserveUserUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        locationRepository = mockk()
        getIncidentByIdUseCase = mockk()
        observeUserUseCase = mockk()
        every { observeUserUseCase() } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun incident(
        id: String,
        status: IncidentStatus = IncidentStatus.OPEN,
        assignedTo: String? = null,
    ) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = status,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = IncidentSeverity.MEDIUM,
        summary = "Calm herd",
        lat = -1.0,
        lng = 29.0,
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    private fun ranger() = User(uid = "r1", email = "r@x.com", displayName = "R", role = UserRole.RANGER, parkId = "p1")

    private fun viewModelFor(id: String): IncidentDetailViewModel =
        IncidentDetailViewModel(
            SavedStateHandle(mapOf("id" to id)),
            incidentRepository,
            locationRepository,
            getIncidentByIdUseCase,
            observeUserUseCase,
        )

    @Test
    fun `resolves the incident matching the route id`() = runTest(testDispatcher) {
        val testIncident = incident("b")
        every { getIncidentByIdUseCase("b") } returns flowOf(testIncident)

        val viewModel = viewModelFor("b")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("b", state.incident?.id)
        }
    }

    @Test
    fun `a ranger can respond to an unassigned open incident`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a"))
        every { observeUserUseCase() } returns MutableStateFlow(ranger())

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertTrue(awaitItem().canRespond)
        }
    }

    @Test
    fun `a ranger cannot respond to an incident already assigned to someone else`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns
            flowOf(incident("a", status = IncidentStatus.IN_PROGRESS, assignedTo = "someone-else"))
        every { observeUserUseCase() } returns MutableStateFlow(ranger())

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAssignedToSomeoneElse)
            assertFalse(state.canRespond)
        }
    }

    @Test
    fun `a ranger cannot respond to a resolved incident`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a", status = IncidentStatus.RESOLVED))
        every { observeUserUseCase() } returns MutableStateFlow(ranger())

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertFalse(awaitItem().canRespond)
        }
    }

    @Test
    fun `respondToIncident claims the incident when not already assigned to the current ranger`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a"))
        every { observeUserUseCase() } returns MutableStateFlow(ranger())
        coEvery { incidentRepository.assignToSelf("a") } returns Unit

        val viewModel = viewModelFor("a")
        // respondToIncident() reads uiState.value, which (like collectAsStateWithLifecycle
        // in the real screen) only reflects live data once something has subscribed.
        viewModel.uiState.test {
            awaitItem()
            viewModel.respondToIncident()
        }

        coVerify { incidentRepository.assignToSelf("a") }
    }

    @Test
    fun `respondToIncident does not re-assign when already assigned to the current ranger`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns
            flowOf(incident("a", status = IncidentStatus.IN_PROGRESS, assignedTo = "r1"))
        every { observeUserUseCase() } returns MutableStateFlow(ranger())

        val viewModel = viewModelFor("a")
        viewModel.uiState.test {
            awaitItem()
            viewModel.respondToIncident()
        }

        coVerify(exactly = 0) { incidentRepository.assignToSelf(any()) }
    }

    @Test
    fun `distance is null initially`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a"))

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertNull(awaitItem().distanceKm)
        }
    }

    @Test
    fun `loadDistance computes distance from current location to the incident`() = runTest(testDispatcher) {
        val testIncident = incident("a") // lat -1.0, lng 29.0
        every { getIncidentByIdUseCase("a") } returns flowOf(testIncident)
        coEvery { incidentRepository.getById("a") } returns testIncident
        // Roughly 1 degree of latitude south of the incident (~111km).
        coEvery { locationRepository.getCurrentLocation() } returns
            Result.success(GeoLocation(latitude = -2.0, longitude = 29.0, accuracyMeters = 5f))

        val viewModel = viewModelFor("a")
        viewModel.loadDistance()

        viewModel.uiState.test {
            val distance = awaitItem().distanceKm
            assertEquals(111.0, distance ?: 0.0, 2.0)
        }
    }

    @Test
    fun `loadDistance leaves distance null when location is unavailable`() = runTest(testDispatcher) {
        val testIncident = incident("a")
        every { getIncidentByIdUseCase("a") } returns flowOf(testIncident)
        coEvery { incidentRepository.getById("a") } returns testIncident
        coEvery { locationRepository.getCurrentLocation() } returns
            Result.failure(IllegalStateException("Location unavailable"))

        val viewModel = viewModelFor("a")
        viewModel.loadDistance()

        viewModel.uiState.test {
            assertNull(awaitItem().distanceKm)
        }
    }
}
