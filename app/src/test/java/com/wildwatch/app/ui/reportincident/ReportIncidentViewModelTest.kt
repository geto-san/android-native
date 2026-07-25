package com.wildwatch.app.ui.reportincident

import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.incident.NewIncidentDetails
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.local.db.SyncStatus
import com.wildwatch.app.data.location.GeoLocation
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.domain.model.Incident
import com.wildwatch.app.sync.SyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportIncidentViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var incidentRepository: IncidentRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var viewModel: ReportIncidentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incidentRepository = mockk()
        locationRepository = mockk()
        syncScheduler = mockk()
        every { syncScheduler.triggerImmediateSync() } returns Unit
        viewModel = ReportIncidentViewModel(incidentRepository, locationRepository, syncScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleIncident(id: String = "inc-1") = Incident(
        id = id,
        type = IncidentType.CONFLICT,
        status = IncidentStatus.OPEN,
        park = Park.MGAHINGA_GORILLA,
        community = "Buhoma",
        species = "Elephant",
        severity = Severity.HIGH,
        summary = null,
        lat = -1.5,
        lng = 29.5,
        locationName = "Buhoma sector",
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        reportedAt = "2026-07-22T00:00:00Z",
        syncStatus = SyncStatus.PENDING,
        lastModified = 1000L,
    )

    @Test
    fun `save with a blank species sets an error and never calls the repository`() {
        viewModel.save()

        assertEquals("Enter what was observed", viewModel.uiState.value.saveError)
        coVerify(exactly = 0) { incidentRepository.create(any()) }
    }

    @Test
    fun `save with a blank community sets an error and never calls the repository`() {
        viewModel.updateSpecies("Elephant")

        viewModel.save()

        assertEquals("Enter the zone or community", viewModel.uiState.value.saveError)
        coVerify(exactly = 0) { incidentRepository.create(any()) }
    }

    @Test
    fun `save without a resolved location sets an error and never calls the repository`() {
        viewModel.updateSpecies("Elephant")
        viewModel.updateCommunity("Buhoma")

        viewModel.save()

        assertEquals("Waiting for GPS signal - please try again", viewModel.uiState.value.saveError)
        coVerify(exactly = 0) { incidentRepository.create(any()) }
    }

    @Test
    fun `loadCurrentLocation success populates coordinates and the reverse-geocoded name`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(-1.5, 29.5, 10f))
        coEvery { locationRepository.reverseGeocode(-1.5, 29.5) } returns "Buhoma sector"

        viewModel.loadCurrentLocation()

        val state = viewModel.uiState.value
        assertEquals(-1.5, state.latitude)
        assertEquals(29.5, state.longitude)
        assertEquals("Buhoma sector", state.locationName)
        assertTrue(!state.isLoadingLocation)
    }

    @Test
    fun `loadCurrentLocation failure surfaces an error and leaves coordinates null`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns Result.failure(Exception("GPS disabled"))

        viewModel.loadCurrentLocation()

        val state = viewModel.uiState.value
        assertEquals("GPS disabled", state.locationError)
        assertNull(state.latitude)
    }

    @Test
    fun `save with valid data and a resolved location creates an incident with the chosen type park and severity`() =
        runTest(testDispatcher) {
            coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(-1.5, 29.5, 10f))
            coEvery { locationRepository.reverseGeocode(-1.5, 29.5) } returns "Buhoma sector"
            val expectedDetails = NewIncidentDetails(
                type = IncidentType.CONFLICT,
                park = Park.MGAHINGA_GORILLA,
                community = "Buhoma",
                species = "Elephant",
                severity = Severity.HIGH,
                category = null,
                summary = null,
                lat = -1.5,
                lng = 29.5,
                locationName = "Buhoma sector",
                localImageUris = emptyList(),
            )
            coEvery { incidentRepository.create(expectedDetails) } returns sampleIncident()

            viewModel.loadCurrentLocation()
            viewModel.updateType(IncidentType.CONFLICT)
            viewModel.updatePark(Park.MGAHINGA_GORILLA)
            viewModel.updateCommunity("Buhoma")
            viewModel.updateSpecies("Elephant")
            viewModel.updateSeverity(Severity.HIGH)
            viewModel.save()

            assertEquals("inc-1", viewModel.uiState.value.savedIncidentId)
            coVerify { incidentRepository.create(expectedDetails) }
        }

    @Test
    fun `addPhoto and removePhoto update the photo list`() {
        viewModel.addPhoto("file:///photo1.jpg")
        viewModel.addPhoto("file:///photo2.jpg")
        assertEquals(listOf("file:///photo1.jpg", "file:///photo2.jpg"), viewModel.uiState.value.photoUris)

        viewModel.removePhoto("file:///photo1.jpg")
        assertEquals(listOf("file:///photo2.jpg"), viewModel.uiState.value.photoUris)
    }
}
