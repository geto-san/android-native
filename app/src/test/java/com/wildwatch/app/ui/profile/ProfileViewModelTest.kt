package com.wildwatch.app.ui.profile

import app.cash.turbine.test
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.domain.model.Incident
import com.wildwatch.app.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var incidentRepository: IncidentRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxUnitFun = true)
        incidentRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun incident(id: String, status: IncidentStatus, assignedTo: String?) = Incident(
        id = id,
        type = IncidentType.SIGHTING,
        status = status,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = "Elephant",
        severity = Severity.MEDIUM,
        lat = -1.5,
        lng = 29.5,
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )

    @Test
    fun `resolved count only includes incidents resolved by the current ranger`() = runTest(testDispatcher) {
        every { authRepository.currentUser } returns MutableStateFlow(
            User(uid = "uid-1", email = "jane@example.com", displayName = "Jane Ranger"),
        )
        every { incidentRepository.observeAll() } returns MutableStateFlow(
            listOf(
                incident("a", IncidentStatus.RESOLVED, assignedTo = "uid-1"),
                incident("b", IncidentStatus.RESOLVED, assignedTo = "uid-2"),
                incident("c", IncidentStatus.IN_PROGRESS, assignedTo = "uid-1"),
            ),
        )

        val viewModel = ProfileViewModel(authRepository, incidentRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Jane Ranger", state.displayName)
            assertEquals("jane@example.com", state.email)
            assertEquals(1, state.resolvedCount)
        }
    }

    @Test
    fun `signOut delegates to the repository`() = runTest(testDispatcher) {
        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { incidentRepository.observeAll() } returns MutableStateFlow(emptyList())
        val viewModel = ProfileViewModel(authRepository, incidentRepository)

        viewModel.signOut()

        verify { authRepository.signOut() }
    }
}
