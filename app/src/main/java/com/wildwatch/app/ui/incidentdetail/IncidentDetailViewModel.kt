package com.wildwatch.app.ui.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.location.GeoLocation
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.data.location.haversineKm
import com.wildwatch.app.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val distanceKm: Double? = null,
    val isLoading: Boolean = true,
)

// Per guardrail G7, only IncidentRepository/LocationRepository are touched
// here. incidentId is read directly off SavedStateHandle under Route
// .IncidentDetail's "id" property name (Navigation Compose's type-safe routes
// store each primitive argument under its property name) rather than via
// SavedStateHandle.toRoute(), which round-trips through a real
// android.os.Bundle internally and isn't usable from a plain JVM unit test.
@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val incidentId: String = checkNotNull(savedStateHandle["id"]) { "Route.IncidentDetail requires an id" }
    private val currentLocation = MutableStateFlow<GeoLocation?>(null)

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        incidentRepository.observeAll().map { incidents -> incidents.find { it.id == incidentId } },
        currentLocation,
    ) { incident, location ->
        val distance = if (incident != null && location != null) {
            haversineKm(location.latitude, location.longitude, incident.lat, incident.lng)
        } else {
            null
        }
        IncidentDetailUiState(incident = incident, distanceKm = distance, isLoading = incident == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentDetailUiState())

    fun loadDistance() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation().onSuccess { currentLocation.value = it }
        }
    }
}
