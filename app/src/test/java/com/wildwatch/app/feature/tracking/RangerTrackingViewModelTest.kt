package com.wildwatch.app.feature.tracking

import com.mapbox.geojson.Point
import com.wildwatch.app.core.data.location.GeoLocation
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.patrol.PatrolRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.database.PatrolStatus
import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import com.wildwatch.app.core.model.PatrolLog
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import com.google.firebase.firestore.GeoPoint
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RangerTrackingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationRepository: LocationRepository = mockk()
    private val parkRepository: ParkRepository = mockk()
    private val getIncidentsUseCase: GetIncidentsUseCase = mockk()
    private val observeUserUseCase: ObserveUserUseCase = mockk()
    private val patrolRepository: PatrolRepository = mockk()
    private lateinit var viewModel: RangerTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { locationRepository.getCurrentLocation() } returns Result.success(
            GeoLocation(0.0, 0.0, 0.0f)
        )
        coEvery { parkRepository.findNearestPark(any(), any()) } returns null
        every { getIncidentsUseCase() } returns flowOf(emptyList())
        every { observeUserUseCase() } returns MutableStateFlow(
            User(uid = "ranger-1", email = "r@x.com", displayName = "R", role = UserRole.RANGER, parkId = "park-1"),
        )
        every { patrolRepository.observeActivePatrol("ranger-1") } returns flowOf(null)
    }

    private fun createViewModel() = RangerTrackingViewModel(
        locationRepository,
        parkRepository,
        getIncidentsUseCase,
        observeUserUseCase,
        patrolRepository,
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `detectActivePark updates location in state`() = runTest {
        val location = GeoLocation(0.5, 0.5, 0.0f)
        coEvery { locationRepository.getCurrentLocation() } returns Result.success(location)

        viewModel = createViewModel()
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

        viewModel = createViewModel()
        viewModel.setActivePark(park)
        advanceUntilIdle()

        assertEquals(park, viewModel.uiState.value.activePark)
    }

    @Test
    fun `toggleMapStyle flips isSatelliteView`() = runTest {
        viewModel = createViewModel()

        val initial = viewModel.uiState.value.isSatelliteView
        viewModel.toggleMapStyle()
        assertEquals(!initial, viewModel.uiState.value.isSatelliteView)
    }

    @Test
    fun `onMapTappedWhilePlacingPoi is ignored unless placing mode is armed`() = runTest {
        viewModel = createViewModel()

        viewModel.onMapTappedWhilePlacingPoi(Point.fromLngLat(30.0, -1.0))

        assertNull(viewModel.uiState.value.pendingPoiPoint)
    }

    @Test
    fun `toggleAddPoiMode arms placing mode then a map tap drops a pending pin`() = runTest {
        viewModel = createViewModel()

        viewModel.toggleAddPoiMode()
        assertTrue(viewModel.uiState.value.isPlacingPoi)

        val tapped = Point.fromLngLat(30.0, -1.0)
        viewModel.onMapTappedWhilePlacingPoi(tapped)

        assertFalse(viewModel.uiState.value.isPlacingPoi)
        assertEquals(tapped, viewModel.uiState.value.pendingPoiPoint)
    }

    @Test
    fun `cancelAddPoi clears the pending pin`() = runTest {
        viewModel = createViewModel()
        viewModel.toggleAddPoiMode()
        viewModel.onMapTappedWhilePlacingPoi(Point.fromLngLat(30.0, -1.0))

        viewModel.cancelAddPoi()

        assertNull(viewModel.uiState.value.pendingPoiPoint)
    }

    @Test
    fun `submitPoi does nothing without an active park`() = runTest {
        viewModel = createViewModel()
        viewModel.toggleAddPoiMode()
        viewModel.onMapTappedWhilePlacingPoi(Point.fromLngLat(30.0, -1.0))

        viewModel.submitPoi("Snare cluster", AttractionType.DANGER_ZONE, "Found 3 wire snares", null)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingPoiPoint)
    }

    @Test
    fun `submitPoi creates the attraction, clears the pin, and reloads attractions`() = runTest {
        val park = NationalPark("park-1", "Bwindi", GeoPoint(0.0, 0.0), emptyList(), "desc")
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())
        val slot = mutableListOf<ParkAttraction>()
        coEvery { parkRepository.createAttraction(capture(slot)) } returns Result.success(Unit)

        viewModel = createViewModel()
        viewModel.setActivePark(park)
        viewModel.toggleAddPoiMode()
        val tapped = Point.fromLngLat(30.5, -1.5)
        viewModel.onMapTappedWhilePlacingPoi(tapped)

        viewModel.submitPoi("Snare cluster", AttractionType.DANGER_ZONE, "Found 3 wire snares", null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingPoiPoint)
        assertFalse(viewModel.uiState.value.isSubmittingPoi)
        assertEquals(1, slot.size)
        assertEquals("park-1", slot.first().parkId)
        assertEquals(AttractionType.DANGER_ZONE, slot.first().type)
        assertEquals("ranger-1", slot.first().reportedBy)
        assertEquals(-1.5, slot.first().location?.latitude ?: 0.0, 0.0001)
        assertEquals(30.5, slot.first().location?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `submitPoi surfaces an error and keeps the pin when the write fails`() = runTest {
        val park = NationalPark("park-1", "Bwindi", GeoPoint(0.0, 0.0), emptyList(), "desc")
        every { parkRepository.getAttractions("park-1") } returns flowOf(emptyList())
        coEvery { parkRepository.createAttraction(any()) } returns Result.failure(IllegalStateException("offline"))

        viewModel = createViewModel()
        viewModel.setActivePark(park)
        viewModel.toggleAddPoiMode()
        viewModel.onMapTappedWhilePlacingPoi(Point.fromLngLat(30.0, -1.0))

        viewModel.submitPoi("Snare cluster", AttractionType.DANGER_ZONE, "desc", null)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.pendingPoiPoint)
        assertFalse(viewModel.uiState.value.isSubmittingPoi)
    }

    @Test
    fun `activePatrol reflects the current ranger's active patrol`() = runTest {
        val patrol = PatrolLog(
            id = "patrol-1",
            rangerUid = "ranger-1",
            parkId = "park-1",
            routePoints = listOf(RoutePoint(-1.0, 30.0, "t1"), RoutePoint(-1.1, 30.1, "t2")),
            startTime = "2026-08-03T00:00:00Z",
            status = PatrolStatus.ACTIVE,
            syncStatus = SyncStatus.PENDING,
        )
        every { patrolRepository.observeActivePatrol("ranger-1") } returns flowOf(patrol)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(patrol, viewModel.uiState.value.activePatrol)
        assertEquals(2, viewModel.uiState.value.patrolRoutePoints.size)
    }

    @Test
    fun `no active patrol yields an empty route`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activePatrol)
        assertTrue(viewModel.uiState.value.patrolRoutePoints.isEmpty())
    }
}
