package com.wildwatch.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RangerTrackingUiState(
    val activePark: NationalPark? = null,
    val attractions: List<ParkAttraction> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val isSatelliteView: Boolean = false,
    val is3DMode: Boolean = false,
    val showAttractions: Boolean = true,
    val showIncidents: Boolean = true,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val userLocation: Point? = null,
    val error: String? = null,
) {
    val mapStyleUri: String
        get() = if (isSatelliteView) {
            "mapbox://styles/mapbox/satellite-streets-v12"
        } else {
            "mapbox://styles/mapbox/streets-v12"
        }
}

@HiltViewModel
class RangerTrackingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val parkRepository: ParkRepository,
    getIncidentsUseCase: GetIncidentsUseCase,
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
    }

    private fun detectActivePark() {
        viewModelScope.launch {
            try {
                locationRepository.getCurrentLocation().onSuccess { location ->
                    val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                    _uiState.update { it.copy(userLocation = userPoint) }

                    val park = parkRepository.findNearestPark(location.latitude, location.longitude)
                    if (park != null) {
                        setActivePark(park)
                    }
                }.onFailure { e ->
                    Timber.e(e, "Failed to get current location")
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearching = false) }
    }
}
