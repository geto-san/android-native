package com.wildwatch.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RangerMapUiState(
    val activePark: NationalPark? = null,
    val attractions: List<ParkAttraction> = emptyList(),
    val isSatelliteView: Boolean = false,
    val is3DMode: Boolean = false,
    val showAttractions: Boolean = true,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val userLocation: Point? = null
)

@HiltViewModel
class RangerMapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val parkRepository: ParkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangerMapUiState())
    val uiState: StateFlow<RangerMapUiState> = _uiState.asStateFlow()

    init {
        detectActivePark()
    }

    private fun detectActivePark() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation().onSuccess { location ->
                val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                _uiState.update { it.copy(userLocation = userPoint) }
                val park = parkRepository.findNearestPark(location.latitude, location.longitude)
                if (park != null) {
                    setActivePark(park)
                }
            }
        }
    }

    fun setActivePark(park: NationalPark) {
        _uiState.update { it.copy(activePark = park) }
        loadAttractions(park.id)
    }

    private fun loadAttractions(parkId: String) {
        parkRepository.getAttractions(parkId)
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearching = false) }
    }
}
