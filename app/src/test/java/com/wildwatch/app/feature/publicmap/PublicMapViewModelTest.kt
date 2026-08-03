package com.wildwatch.app.feature.publicmap

import com.google.firebase.firestore.GeoPoint
import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class PublicMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationRepository: LocationRepository = mockk()
    private val parkRepository: ParkRepository = mockk()
    private lateinit var viewModel: PublicMapViewModel

    private val park = NationalPark("park-1", "Bwindi", GeoPoint(0.0, 0.0), emptyList(), "desc")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(GeoLocation(0.0, 0.0, 0.0f))
        coEvery { parkRepository.findNearestPark(any(), any()) } returns park
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun attraction(type: AttractionType, name: String = type.name) = ParkAttraction(
        id = name,
        parkId = "park-1",
        name = name,
        type = type,
        location = GeoPoint(0.0, 0.0),
    )

    @Test
    fun `danger zones are filtered out of the public map`() = runTest(testDispatcher) {
        every { parkRepository.getAttractions("park-1") } returns flowOf(
            listOf(
                attraction(AttractionType.DANGER_ZONE),
                attraction(AttractionType.VIEWPOINT),
                attraction(AttractionType.WATER_SOURCE),
            ),
        )

        viewModel = PublicMapViewModel(locationRepository, parkRepository)
        advanceUntilIdle()

        val shown = viewModel.uiState.value.attractions
        assertEquals(2, shown.size)
        assertTrue(shown.none { it.type == AttractionType.DANGER_ZONE })
    }

    @Test
    fun `detects the nearest park from the device location on init`() = runTest(testDispatcher) {
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())

        viewModel = PublicMapViewModel(locationRepository, parkRepository)
        advanceUntilIdle()

        assertEquals(park, viewModel.uiState.value.activePark)
    }

    @Test
    fun `falls back to the first park when location is unavailable`() = runTest(testDispatcher) {
        coEvery { locationRepository.getCurrentLocation() } returns
            Result.failure(IllegalStateException("Location permission not granted"))
        every { parkRepository.getParks() } returns flowOf(listOf(park))
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())

        viewModel = PublicMapViewModel(locationRepository, parkRepository)
        advanceUntilIdle()

        assertEquals(park, viewModel.uiState.value.activePark)
    }

    @Test
    fun `toggleMapStyle flips isSatelliteView`() = runTest(testDispatcher) {
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())
        viewModel = PublicMapViewModel(locationRepository, parkRepository)

        val initial = viewModel.uiState.value.isSatelliteView
        viewModel.toggleMapStyle()

        assertFalse(initial)
        assertTrue(viewModel.uiState.value.isSatelliteView)
    }
}
