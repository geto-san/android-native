package com.wildwatch.app.feature.tracking

import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.model.NationalPark
import com.google.firebase.firestore.GeoPoint
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RangerTrackingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationRepository: LocationRepository = mockk()
    private val parkRepository: ParkRepository = mockk()
    private lateinit var viewModel: RangerTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(
            GeoLocation(0.0, 0.0, 0.0f)
        )
        coEvery { parkRepository.findNearestPark(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `detectActivePark updates location in state`() = runTest {
        val location = GeoLocation(0.5, 0.5, 0.0f)
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(location)

        viewModel = RangerTrackingViewModel(locationRepository, parkRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.userLocation)
        assertEquals(0.5, state.userLocation?.longitude() ?: 0.0, 0.0001)
        assertEquals(0.5, state.userLocation?.latitude() ?: 0.0, 0.0001)
    }

    @Test
    fun `setActivePark updates park and loads attractions`() = runTest {
        val park = NationalPark("park-1", "Bwindi", GeoPoint(0.0, 0.0), emptyList(), "desc")
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())

        viewModel = RangerTrackingViewModel(locationRepository, parkRepository)
        viewModel.setActivePark(park)
        advanceUntilIdle()

        assertEquals(park, viewModel.uiState.value.activePark)
    }

    @Test
    fun `toggleMapStyle flips isSatelliteView`() = runTest {
        viewModel = RangerTrackingViewModel(locationRepository, parkRepository)
        
        val initial = viewModel.uiState.value.isSatelliteView
        viewModel.toggleMapStyle()
        assertEquals(!initial, viewModel.uiState.value.isSatelliteView)
    }
}
