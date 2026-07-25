package com.wildwatch.app.ui.tracking

import app.cash.turbine.test
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.RangerProgress
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.domain.model.Incident
import com.wildwatch.app.domain.model.User
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
class TrackingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        authRepository = mockk()
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun incident(
        id: String,
        assignedTo: String? = "uid-1",
        status: IncidentStatus = IncidentStatus.IN_PROGRESS,
        rangerProgress: RangerProgress? = RangerProgress.EN_ROUTE,
        severity: Severity = Severity.MEDIUM,
        isEscalated: Boolean = false,
    ) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = status,
        rangerProgress = rangerProgress,
        isEscalated = isEscalated,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = severity,
        lat = -1.5,
        lng = 29.5,
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    @Test
    fun `only incidents assigned to the current ranger are included`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(incident("mine", assignedTo = "uid-1"), incident("theirs", assignedTo = "uid-2")),
        )

        val viewModel = TrackingViewModel(incidentRepository, authRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.incidents.size)
            assertEquals("mine", state.incidents.first().id)
        }
    }

    @Test
    fun `urgent filter keeps only high and critical severity`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(
                incident("low", severity = Severity.LOW),
                incident("high", severity = Severity.HIGH),
                incident("critical", severity = Severity.CRITICAL),
            ),
        )

        val viewModel = TrackingViewModel(incidentRepository, authRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.selectFilter(TrackingFilter.URGENT)
            val state = awaitItem()
            assertEquals(setOf("high", "critical"), state.incidents.map { it.id }.toSet())
        }
    }

    @Test
    fun `escalated filter keeps only escalated incidents`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(incident("a", isEscalated = true), incident("b", isEscalated = false)),
        )

        val viewModel = TrackingViewModel(incidentRepository, authRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.selectFilter(TrackingFilter.ESCALATED)
            val state = awaitItem()
            assertEquals(listOf("a"), state.incidents.map { it.id })
        }
    }

    @Test
    fun `resolved filter keeps only resolved incidents`() = runTest(testDispatcher) {
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(
                incident("done", status = IncidentStatus.RESOLVED, rangerProgress = null),
                incident("active", status = IncidentStatus.IN_PROGRESS),
            ),
        )

        val viewModel = TrackingViewModel(incidentRepository, authRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.selectFilter(TrackingFilter.RESOLVED)
            val state = awaitItem()
            assertEquals(listOf("done"), state.incidents.map { it.id })
        }
    }
}
