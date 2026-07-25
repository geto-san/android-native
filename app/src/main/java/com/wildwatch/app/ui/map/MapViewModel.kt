package com.wildwatch.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.location.GeoLocation
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val incidents: List<Incident> = emptyList(),
    val currentLocation: GeoLocation? = null,
)

// Per guardrail G7, only LocationRepository/IncidentRepository are touched
// here - no FusedLocationProviderClient, no Room, no Firestore.
@HiltViewModel
class MapViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val currentLocation = MutableStateFlow<GeoLocation?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        incidentRepository.observeAll(),
        currentLocation,
    ) { incidents, location -> MapUiState(incidents, location) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())

    fun loadCurrentLocation() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation().onSuccess { currentLocation.value = it }
        }
    }
}
