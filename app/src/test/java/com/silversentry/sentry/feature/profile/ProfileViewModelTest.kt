package com.silversentry.sentry.feature.profile

import app.cash.turbine.test
import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.user.UserDataRepository
import com.silversentry.sentry.core.database.IncidentSeverity
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.database.Park
import com.silversentry.sentry.core.database.SyncStatus
import com.silversentry.sentry.core.domain.usecase.GetIncidentsUseCase
import com.silversentry.sentry.core.domain.usecase.ObserveUserUseCase
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.model.User
import com.silversentry.sentry.core.model.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var userDataRepository: UserDataRepository
    private lateinit var observeUserUseCase: ObserveUserUseCase
    private lateinit var getIncidentsUseCase: GetIncidentsUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxUnitFun = true)
        userDataRepository = mockk()
        observeUserUseCase = mockk()
        getIncidentsUseCase = mockk()
        every { userDataRepository.darkThemeConfig } returns flowOf(false)
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
        severity = IncidentSeverity.MEDIUM,
        lat = -1.5,
        lng = 29.5,
        reportedAt = "2026-07-22T00:00:00Z",
        assignedTo = assignedTo,
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L
    )

    @Test
    fun `resolved count only includes incidents resolved by the current ranger`() = runTest(testDispatcher) {
        val user = User(
            uid = "uid-1", 
            email = "jane@example.com", 
            displayName = "Jane Ranger",
            role = UserRole.RANGER
        )
        every { observeUserUseCase() } returns MutableStateFlow(user)
        
        val incidents = listOf(
            incident("a", IncidentStatus.RESOLVED, assignedTo = "uid-1"),
            incident("b", IncidentStatus.RESOLVED, assignedTo = "uid-2"),
            incident("c", IncidentStatus.IN_PROGRESS, assignedTo = "uid-1"),
        )
        every { getIncidentsUseCase() } returns flowOf(incidents)

        val viewModel = ProfileViewModel(authRepository, userDataRepository, observeUserUseCase, getIncidentsUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Jane Ranger", state.displayName)
            assertEquals("jane@example.com", state.email)
            assertEquals(1, state.resolvedCount)
        }
    }

    @Test
    fun `signOut delegates to the repository`() = runTest(testDispatcher) {
        every { observeUserUseCase() } returns MutableStateFlow(null)
        every { getIncidentsUseCase() } returns flowOf(emptyList())
        val viewModel = ProfileViewModel(authRepository, userDataRepository, observeUserUseCase, getIncidentsUseCase)

        viewModel.signOut()

        verify { authRepository.signOut() }
    }
}
