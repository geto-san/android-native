package com.wildwatch.app.core.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wildwatch.app.core.data.incident.IncidentRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var getIncidentByIdUseCase: GetIncidentByIdUseCase
    private lateinit var observeUserUseCase: ObserveUserUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        getIncidentByIdUseCase = mockk()
        observeUserUseCase = mockk()
        every { observeUserUseCase() } returns MutableStateFlow(null)
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
        severity = IncidentSeverity.MEDIUM,
        summary = "Calm herd",
        lat = -1.0,
        lng = 29.0,
        reportedAt = "2026-07-22T00:00:00Z",
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    private fun viewModelFor(id: String): IncidentDetailViewModel =
        IncidentDetailViewModel(
            SavedStateHandle(mapOf("id" to id)),
            incidentRepository,
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
    fun `rangers cannot self-assign`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a"))
        every { observeUserUseCase() } returns MutableStateFlow(
            User(uid = "r1", email = "r@x.com", displayName = "R", role = UserRole.RANGER, parkId = "p1"),
        )

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertFalse(awaitItem().canAssignToSelf)
        }
    }

    @Test
    fun `distance is null initially`() = runTest(testDispatcher) {
        every { getIncidentByIdUseCase("a") } returns flowOf(incident("a"))

        val viewModel = viewModelFor("a")

        viewModel.uiState.test {
            assertNull(awaitItem().distanceKm)
        }
    }
}
