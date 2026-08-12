package com.wildwatch.app.feature.report

import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.sync.SyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportIncidentViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var viewModel: ReportIncidentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        locationRepository = mockk()
        syncScheduler = mockk(relaxUnitFun = true)
        notificationRepository = mockk(relaxUnitFun = true)
        coEvery { locationRepository.getCurrentLocation() } returns
            Result.success(GeoLocation(latitude = -1.05, longitude = 29.7, accuracyMeters = 5f))
        coEvery { locationRepository.reverseGeocode(any(), any()) } returns "Buhoma"
        every { locationRepository.getParkFromLocation(any(), any()) } returns "Bwindi Impenetrable"
        viewModel = ReportIncidentViewModel(incidentRepository, locationRepository, syncScheduler, notificationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize for a new report auto-captures location and park`() = runTest(testDispatcher) {
        viewModel.initialize(draftId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(-1.05, state.lat, 0.0001)
        assertEquals(29.7, state.lng, 0.0001)
        assertEquals("Buhoma", state.locationName)
        assertEquals(Park.BWINDI_IMPENETRABLE, state.park)
        assertFalse(state.isLocationLoading)
    }

    @Test
    fun `initialize with a draftId loads the existing incident's fields`() = runTest(testDispatcher) {
        val draft = incident(id = "draft-1", type = IncidentType.CONFLICT, species = "N/A", summary = "Fence damaged")
        coEvery { incidentRepository.getById("draft-1") } returns draft

        viewModel.initialize(draftId = "draft-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(IncidentType.CONFLICT, state.type)
        assertEquals("Fence damaged", state.description)
    }

    @Test
    fun `canSubmit requires species only for sighting reports`() {
        viewModel.selectType(IncidentType.SIGHTING)
        viewModel.updateDescription("Saw a herd near the trail")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateSpecies("Elephant")
        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.selectType(IncidentType.CONFLICT)
        assertTrue(viewModel.uiState.value.canSubmit) // description alone is enough for non-sighting types
    }

    @Test
    fun `save on a new report creates an incident and triggers sync`() = runTest(testDispatcher) {
        val created = incident(id = "new-1", type = IncidentType.CONFLICT, species = "N/A", summary = "Crop damage")
        coEvery { incidentRepository.create(any(), false) } returns created

        viewModel.initialize(draftId = null)
        advanceUntilIdle()
        viewModel.selectType(IncidentType.CONFLICT)
        viewModel.updateDescription("Crop damage")

        viewModel.save()
        advanceUntilIdle()

        assertEquals("new-1", viewModel.uiState.value.savedIncidentId)
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify {
            incidentRepository.create(
                match<NewIncidentDetails> { it.type == IncidentType.CONFLICT && it.summary == "Crop damage" },
                false,
            )
        }
        coVerify { syncScheduler.triggerImmediateSync() }
        coVerify { notificationRepository.notifyPendingSync("new-1") }
    }

    @Test
    fun `save as draft skips sync and notification`() = runTest(testDispatcher) {
        val created = incident(id = "new-2", type = IncidentType.SIGHTING, species = "Elephant", summary = "")
        coEvery { incidentRepository.create(any(), true) } returns created

        viewModel.initialize(draftId = null)
        advanceUntilIdle()
        viewModel.updateSpecies("Elephant")

        viewModel.save(asDraft = true)
        advanceUntilIdle()

        assertEquals("new-2", viewModel.uiState.value.savedIncidentId)
        coVerify(exactly = 0) { syncScheduler.triggerImmediateSync() }
        coVerify(exactly = 0) { notificationRepository.notifyPendingSync(any()) }
    }

    @Test
    fun `save on an existing draft updates rather than creates`() = runTest(testDispatcher) {
        val draft = incident(id = "draft-3", type = IncidentType.SIGHTING, species = "Buffalo", summary = "Herd sighting")
        coEvery { incidentRepository.getById("draft-3") } returns draft
        coEvery { incidentRepository.update("draft-3", any(), false) } returns Unit

        viewModel.initialize(draftId = "draft-3")
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals("draft-3", viewModel.uiState.value.savedIncidentId)
        coVerify { incidentRepository.update("draft-3", any(), false) }
        coVerify(exactly = 0) { incidentRepository.create(any(), any()) }
    }

    @Test
    fun `location failure surfaces an error without crashing and still allows submission`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns Result.failure(Exception("GPS unavailable"))

        viewModel.initialize(draftId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLocationLoading)
        assertEquals("GPS unavailable", state.locationError)

        viewModel.selectType(IncidentType.CONFLICT)
        viewModel.updateDescription("Saw smoke near the ranger post")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    private fun incident(
        id: String,
        type: IncidentType,
        species: String,
        summary: String,
    ) = Incident(
        id = id,
        type = type,
        status = IncidentStatus.OPEN,
        park = Park.BWINDI_IMPENETRABLE,
        community = "Buhoma",
        species = species,
        severity = IncidentSeverity.MEDIUM,
        summary = summary,
        lat = -1.05,
        lng = 29.7,
        reportedAt = "2026-08-12T00:00:00Z",
        syncStatus = SyncStatus.SYNCED,
        lastModified = 1000L,
    )
}
