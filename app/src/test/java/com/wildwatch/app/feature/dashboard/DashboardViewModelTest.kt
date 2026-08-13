package com.wildwatch.app.feature.dashboard

import app.cash.turbine.test
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.model.Incident
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var getIncidentsUseCase: GetIncidentsUseCase
    private lateinit var notificationRepository: NotificationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connectivityObserver = mockk()
        getIncidentsUseCase = mockk()
        notificationRepository = mockk()
        every { connectivityObserver.isOnline } returns MutableStateFlow(true)
        every { notificationRepository.observeUnreadCount() } returns flowOf(0)
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
        severity: IncidentSeverity = IncidentSeverity.MEDIUM,
        reportedAt: String = "2026-07-22T00:00:00Z",
    ) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = status,
        park = Park.BWINDI_IMPENETRABLE,
        community = community,
        species = "Elephant",
        severity = severity,
        lat = -1.5,
        lng = 29.5,
        reportedAt = reportedAt,
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    @Test
    fun `lists are derived correctly from the incident list`() = runTest(testDispatcher) {
        val incidents = listOf(
            incident("a", status = IncidentStatus.OPEN, community = "Buhoma"),
            incident("b", status = IncidentStatus.RESOLVED, community = "Buhoma"),
            incident("c", status = IncidentStatus.IN_PROGRESS, community = "Nkuringo", assignedTo = "uid-2"),
        )
        every { getIncidentsUseCase() } returns flowOf(incidents)

        val viewModel = DashboardViewModel(getIncidentsUseCase, connectivityObserver, notificationRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Buhoma", "Nkuringo"), state.zones)
            assertEquals(1, state.activeIncidents.size)
            assertEquals("c", state.activeIncidents.first().id)
            assertEquals(1, state.activeAlerts.size)
            assertEquals("a", state.activeAlerts.first().id)
        }
    }

    @Test
    fun `active alerts are sorted most-severe then most-recent first`() = runTest(testDispatcher) {
        val incidents = listOf(
            incident("low-old", severity = IncidentSeverity.LOW, reportedAt = "2026-07-01T00:00:00Z"),
            incident("high-old", severity = IncidentSeverity.HIGH, reportedAt = "2026-07-01T00:00:00Z"),
            incident("high-new", severity = IncidentSeverity.HIGH, reportedAt = "2026-07-20T00:00:00Z"),
            incident("medium", severity = IncidentSeverity.MEDIUM, reportedAt = "2026-07-10T00:00:00Z"),
        )
        every { getIncidentsUseCase() } returns flowOf(incidents)

        val viewModel = DashboardViewModel(getIncidentsUseCase, connectivityObserver, notificationRepository)

        viewModel.uiState.test {
            val order = awaitItem().activeAlerts.map { it.id }
            assertEquals(listOf("high-new", "high-old", "medium", "low-old"), order)
        }
    }

    @Test
    fun `selecting a zone scopes the incident and alert lists`() = runTest(testDispatcher) {
        val incidents = listOf(
            incident("a", status = IncidentStatus.OPEN, community = "Buhoma"),
            incident("b", status = IncidentStatus.OPEN, community = "Nkuringo"),
        )
        every { getIncidentsUseCase() } returns flowOf(incidents)

        val viewModel = DashboardViewModel(getIncidentsUseCase, connectivityObserver, notificationRepository)

        viewModel.uiState.test {
            assertEquals(2, awaitItem().activeAlerts.size)

            viewModel.selectZone("Buhoma")

            val scoped = awaitItem()
            assertEquals(1, scoped.activeAlerts.size)
            assertEquals("a", scoped.activeAlerts.first().id)
        }
    }
}
