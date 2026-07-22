package com.silverback.sentry.ui.addsighting

import com.silverback.sentry.data.location.GeoLocation
import com.silverback.sentry.data.location.LocationRepository
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.domain.model.Observation
import com.silverback.sentry.sync.SyncScheduler
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
class AddSightingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var observationRepository: ObservationRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var viewModel: AddSightingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observationRepository = mockk()
        locationRepository = mockk()
        syncScheduler = mockk()
        every { syncScheduler.triggerImmediateSync() } returns Unit
        viewModel = AddSightingViewModel(observationRepository, locationRepository, syncScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleObservation(id: String = "obs-1") = Observation(
        id = id,
        gorillaGroup = "Susa Group",
        location = "-1.5, 29.5",
        locationName = "Volcanoes National Park",
        healthStatus = "Healthy",
        notes = null,
        userName = "Jane Ranger",
        userEmail = "jane@example.com",
        userId = "uid-1",
        createdAt = "2026-07-22T00:00:00Z",
        observationStatus = ObservationStatus.PENDING,
        syncStatus = SyncStatus.PENDING,
        lastModified = 1000L,
    )

    @Test
    fun `save with a blank gorilla group sets an error and never calls the repository`() {
        viewModel.save()

        assertEquals("Enter the gorilla group name", viewModel.uiState.value.saveError)
        coVerify(exactly = 0) { observationRepository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save without a resolved location sets an error and never calls the repository`() {
        viewModel.updateGorillaGroup("Susa Group")

        viewModel.save()

        assertEquals("Waiting for GPS signal - please try again", viewModel.uiState.value.saveError)
        coVerify(exactly = 0) { observationRepository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `loadCurrentLocation success populates coordinates and the reverse-geocoded name`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(-1.5, 29.5, 10f))
        coEvery { locationRepository.reverseGeocode(-1.5, 29.5) } returns "Volcanoes National Park"

        viewModel.loadCurrentLocation()

        val state = viewModel.uiState.value
        assertEquals(-1.5, state.latitude)
        assertEquals(29.5, state.longitude)
        assertEquals("Volcanoes National Park", state.locationName)
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
    fun `save with valid data and a resolved location creates an observation`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(-1.5, 29.5, 10f))
        coEvery { locationRepository.reverseGeocode(-1.5, 29.5) } returns "Volcanoes National Park"
        coEvery {
            observationRepository.create(
                gorillaGroup = "Susa Group",
                location = "-1.5, 29.5",
                locationName = "Volcanoes National Park",
                healthStatus = null,
                notes = null,
                localImageUris = emptyList(),
            )
        } returns sampleObservation()

        viewModel.loadCurrentLocation()
        viewModel.updateGorillaGroup("Susa Group")
        viewModel.save()

        assertEquals("obs-1", viewModel.uiState.value.savedObservationId)
        coVerify {
            observationRepository.create(
                gorillaGroup = "Susa Group",
                location = "-1.5, 29.5",
                locationName = "Volcanoes National Park",
                healthStatus = null,
                notes = null,
                localImageUris = emptyList(),
            )
        }
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
