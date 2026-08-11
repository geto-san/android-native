package com.wildwatch.app.feature.publicmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.repository.ParkRepository
import com.wildwatch.app.core.model.AttractionType
import com.wildwatch.app.core.model.NationalPark
import com.wildwatch.app.core.model.ParkAttraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PublicMapUiState(
    val activePark: NationalPark? = null,
    val attractions: List<ParkAttraction> = emptyList(),
    val isSatelliteView: Boolean = false,
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

// Read-only map for public/tourist users - deliberately a separate, much
// smaller ViewModel from RangerTrackingViewModel rather than a "read-only
// mode" flag bolted onto it: no incidents (conflict/poaching data isn't
// public-safe), no danger-zone attractions, no add-POI or patrol-tracking
// controls, no ranger role check needed since this is the public-facing tab.
@HiltViewModel
class PublicMapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val parkRepository: ParkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicMapUiState())
    val uiState: StateFlow<PublicMapUiState> = _uiState.asStateFlow()

    init {
        detectActivePark()
    }

    private fun detectActivePark() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                    _uiState.update { it.copy(userLocation = userPoint) }

                    parkRepository.findNearestPark(location.latitude, location.longitude)?.let(::setActivePark)
                        ?: fallBackToFirstPark()
                }
                .onFailure { e ->
                    // No location permission yet, or GPS unavailable - a public
                    // user without location access should still get a browsable
                    // map, not a permanently blank one.
                    Timber.w(e, "Failed to get current location for public map - falling back to first park")
                    fallBackToFirstPark()
                }
        }
    }

    private suspend fun fallBackToFirstPark() {
        runCatching { parkRepository.getParks().first() }
            .getOrNull()
            ?.firstOrNull()
            ?.let(::setActivePark)
    }

    fun setActivePark(park: NationalPark) {
        _uiState.update { it.copy(activePark = park) }
        loadAttractions(park.id)
    }

    private fun loadAttractions(parkId: String) {
        parkRepository.getAttractions(parkId)
            // Danger zones are ranger-facing safety flags (e.g. active snare
            // lines, poaching hotspots) - surfacing them to public/tourist
            // users would be actively unsafe/counterproductive, not just noise.
            .map { attractions -> attractions.filter { it.type != AttractionType.DANGER_ZONE } }
            .catch { e ->
                Timber.e(e, "Failed to load attractions for park: $parkId")
                _uiState.update { it.copy(error = "Failed to load park features") }
            }
            .onEach { list -> _uiState.update { it.copy(attractions = list) } }
            .launchIn(viewModelScope)
    }

    fun toggleMapStyle() {
        _uiState.update { it.copy(isSatelliteView = !it.isSatelliteView) }
    }
}
