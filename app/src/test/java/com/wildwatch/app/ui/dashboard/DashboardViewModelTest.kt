package com.wildwatch.app.ui.dashboard

import app.cash.turbine.test
import com.wildwatch.app.data.connectivity.ConnectivityObserver
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.domain.model.Incident
import io.mockk.coEvery
import io.mockk.coVerify
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
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var connectivityObserver: ConnectivityObserver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        connectivityObserver = mockk()
        every { connectivityObserver.isOnline } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun incident(
        id: String,
        status: IncidentStatus = IncidentStatus.OPEN,
        community: String = "Buhoma",
        assignedTo: String? = null,
    ) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = status,
        park = Park.BWINDI_IMPENETRABLE,
        community = community,
        species = "Elephant",
        severity = Severity.MEDIUM,
        summary = null,
        lat = -1.5,
        lng = 29.5,
        locationName = "$community sector",
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    @Test
    fun `stats and lists are derived correctly from the incident list`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(
                incident("a", status = IncidentStatus.OPEN, community = "Buhoma"),
                incident("b", status = IncidentStatus.RESOLVED, community = "Buhoma"),
                incident("c", status = IncidentStatus.IN_PROGRESS, community = "Nkuringo", assignedTo = "uid-2"),
            ),
        )

        val viewModel = DashboardViewModel(incidentRepository, connectivityObserver)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.resolvedCount)
            assertEquals(1, state.pendingCount)
            assertEquals(2, state.activeZoneCount)
            assertEquals(listOf("Buhoma", "Nkuringo"), state.zones)
            assertEquals(1, state.activeIncidents.size)
            assertEquals("c", state.activeIncidents.first().id)
            assertEquals(1, state.activeAlerts.size)
            assertEquals("a", state.activeAlerts.first().id)
        }
    }

    @Test
    fun `selecting a zone scopes the incident and alert lists but not the hero stats`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(
                incident("a", status = IncidentStatus.OPEN, community = "Buhoma"),
                incident("b", status = IncidentStatus.OPEN, community = "Nkuringo"),
            ),
        )

        val viewModel = DashboardViewModel(incidentRepository, connectivityObserver)

        viewModel.uiState.test {
            assertEquals(2, awaitItem().activeAlerts.size)

            viewModel.selectZone("Buhoma")

            val scoped = awaitItem()
            assertEquals(1, scoped.activeAlerts.size)
            assertEquals("a", scoped.activeAlerts.first().id)
            assertEquals(2, scoped.pendingCount) // hero stats stay global
        }
    }

    @Test
    fun `assignToSelf delegates to the repository`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { incidentRepository.assignToSelf("a") } returns Unit
        val viewModel = DashboardViewModel(incidentRepository, connectivityObserver)

        viewModel.assignToSelf("a")

        coVerify { incidentRepository.assignToSelf("a") }
    }
}
