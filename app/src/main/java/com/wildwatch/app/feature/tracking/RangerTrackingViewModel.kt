package com.wildwatch.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.patrol.PatrolRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import com.wildwatch.app.core.model.PatrolLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

data class RangerTrackingUiState(
    val activePark: NationalPark? = null,
    val attractions: List<ParkAttraction> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val isSatelliteView: Boolean = false,
    val is3DMode: Boolean = false,
    val showAttractions: Boolean = true,
    val showIncidents: Boolean = true,
    val userLocation: Point? = null,
    val error: String? = null,
    // Add-POI / danger-zone flow: armed = next map tap drops a pending pin;
    // pendingPoiPoint != null shows the entry form for that pin.
    val isPlacingPoi: Boolean = false,
    val pendingPoiPoint: Point? = null,
    val isSubmittingPoi: Boolean = false,
    val activePatrol: PatrolLog? = null,
) {
    val patrolRoutePoints: List<Point>
        get() = activePatrol?.routePoints?.map { Point.fromLngLat(it.lng, it.lat) } ?: emptyList()
    val mapStyleUri: String
        get() = if (isSatelliteView) {
            "mapbox://styles/mapbox/satellite-streets-v12"
        } else {
            "mapbox://styles/mapbox/streets-v12"
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RangerTrackingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val parkRepository: ParkRepository,
    getIncidentsUseCase: GetIncidentsUseCase,
    private val observeUserUseCase: ObserveUserUseCase,
    private val patrolRepository: PatrolRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangerTrackingUiState())
    val uiState: StateFlow<RangerTrackingUiState> = _uiState.asStateFlow()

    init {
        detectActivePark()
        getIncidentsUseCase()
            .map { incidents ->
                incidents.filter {
                    it.syncStatus == SyncStatus.SYNCED && it.lat != 0.0 && it.lng != 0.0
                }
            }
            .onEach { filtered -> _uiState.update { it.copy(incidents = filtered) } }
            .launchIn(viewModelScope)

        observeUserUseCase()
            .map { it?.uid }
            .distinctUntilChanged()
            .flatMapLatest { uid -> uid?.let(patrolRepository::observeActivePatrol) ?: flowOf(null) }
            .onEach { patrol -> _uiState.update { it.copy(activePatrol = patrol) } }
            .launchIn(viewModelScope)
    }

    // The map centers on (and, via GetIncidentsUseCase, the incident list is scoped to) the
    // ranger's server-assigned park (User.parkId), not GPS-nearest - the same "server-
    // assigned, not GPS" call already made for offline map prefetch (see
    // OfflineMapCoordinator). GPS-nearest is now only a fallback for the rare case a ranger
    // has no park assigned yet, so the map isn't just blank.
    private fun detectActivePark() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                    .onFailure { e -> Timber.e(e, "Failed to get current location") }
                    .getOrNull()
                location?.let {
                    _uiState.update { state -> state.copy(userLocation = Point.fromLngLat(it.longitude, it.latitude)) }
                }

                val assignedParkId = observeUserUseCase().value?.parkId
                val park = assignedParkId?.takeIf { it.isNotBlank() }?.let { parkRepository.getPark(it) }
                    ?: location?.let { parkRepository.findNearestPark(it.latitude, it.longitude) }
                if (park != null) {
                    setActivePark(park)
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error in detectActivePark")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setActivePark(park: NationalPark) {
        _uiState.update { it.copy(activePark = park) }
        loadAttractions(park.id)
    }

    private fun loadAttractions(parkId: String) {
        parkRepository.getAttractions(parkId)
            .catch { e ->
                Timber.e(e, "Failed to load attractions for park: $parkId")
                _uiState.update { it.copy(error = "Failed to load park features") }
            }
            .onEach { list ->
                _uiState.update { it.copy(attractions = list) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleMapStyle() {
        _uiState.update { it.copy(isSatelliteView = !it.isSatelliteView) }
    }

    fun toggle3DMode() {
        _uiState.update { it.copy(is3DMode = !it.is3DMode) }
    }

    fun toggleAttractionsVisibility() {
        _uiState.update { it.copy(showAttractions = !it.showAttractions) }
    }

    fun toggleIncidentsVisibility() {
        _uiState.update { it.copy(showIncidents = !it.showIncidents) }
    }

    fun toggleAddPoiMode() {
        _uiState.update { it.copy(isPlacingPoi = !it.isPlacingPoi, pendingPoiPoint = null) }
    }

    // Called from the map's tap listener only while isPlacingPoi is armed -
    // drops the pending pin and opens the entry form for it.
    fun onMapTappedWhilePlacingPoi(point: Point) {
        if (!_uiState.value.isPlacingPoi) return
        _uiState.update { it.copy(isPlacingPoi = false, pendingPoiPoint = point) }
    }

    fun cancelAddPoi() {
        _uiState.update { it.copy(pendingPoiPoint = null) }
    }

    fun submitPoi(name: String, type: AttractionType, description: String, animalSpecies: String?) {
        val point = _uiState.value.pendingPoiPoint
        val park = _uiState.value.activePark
        if (point == null || park == null || name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPoi = true) }
            val attraction = ParkAttraction(
                parkId = park.id,
                name = name.trim(),
                type = type,
                location = GeoPoint(point.latitude(), point.longitude()),
                description = description.trim(),
                animalSpecies = animalSpecies?.trim()?.takeIf { it.isNotBlank() },
                reportedBy = observeUserUseCase().value?.uid,
                createdAt = Instant.now().toString(),
            )
            parkRepository.createAttraction(attraction)
                .onSuccess {
                    _uiState.update { it.copy(isSubmittingPoi = false, pendingPoiPoint = null) }
                    loadAttractions(park.id)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to create attraction for park: ${park.id}")
                    _uiState.update {
                        it.copy(isSubmittingPoi = false, error = "Failed to save point of interest")
                    }
                }
        }
    }
}
