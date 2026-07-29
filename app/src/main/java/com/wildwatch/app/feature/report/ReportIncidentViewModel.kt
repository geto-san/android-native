package com.wildwatch.app.feature.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val type: IncidentType = IncidentType.SIGHTING,
    val park: Park = Park.BWINDI_IMPENETRABLE,
    val community: String = "",
    val species: String = "",
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val category: String? = null,
    val summary: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val locationName: String? = null,
    val localImageUris: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedIncidentId: String? = null,
    val isLocationLoading: Boolean = false,
    val canSubmit: Boolean = false,
)

@HiltViewModel
class ReportIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    // Persisted fields for process death resilience
    var species: String
        get() = savedStateHandle["species"] ?: ""
        set(value) {
            savedStateHandle["species"] = value
            validate()
        }

    var numberObserved: String
        get() = savedStateHandle["numberObserved"] ?: ""
        set(value) {
            savedStateHandle["numberObserved"] = value
        }

    var behavior: String
        get() = savedStateHandle["behavior"] ?: ""
        set(value) {
            savedStateHandle["behavior"] = value
        }

    var description: String
        get() = savedStateHandle["description"] ?: ""
        set(value) {
            savedStateHandle["description"] = value
            validate()
        }

    fun updateType(type: IncidentType) { _uiState.update { it.copy(type = type) } }
    fun updateCommunity(community: String) { _uiState.update { it.copy(community = community) } }
    fun updateSeverity(severity: IncidentSeverity) { _uiState.update { it.copy(severity = severity) } }

    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocationLoading = true) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            lat = location.latitude,
                            lng = location.longitude,
                            locationName = name,
                            isLocationLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLocationLoading = false, saveError = "Failed to get location: ${error.message}") }
                }
        }
    }

    private fun validate() {
        _uiState.update { it.copy(canSubmit = species.isNotBlank() && description.isNotBlank()) }
    }

    fun addPhoto(uri: String) {
        _uiState.update {
            val newUris = it.localImageUris + uri
            it.copy(localImageUris = newUris, photoUris = newUris)
        }
    }

    fun removePhoto(uri: String) {
        _uiState.update {
            val newUris = it.localImageUris.filter { u -> u != uri }
            it.copy(localImageUris = newUris, photoUris = newUris)
        }
    }

    fun save() {
        if (!_uiState.value.canSubmit) return

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val countText = numberObserved.ifBlank { null }?.let { "Observed $it. " } ?: ""
                val incident = incidentRepository.create(
                    NewIncidentDetails(
                        type = state.type,
                        park = state.park,
                        community = state.community,
                        species = species,
                        severity = state.severity,
                        category = behavior,
                        summary = "$countText$description".trim(),
                        lat = state.lat,
                        lng = state.lng,
                        locationName = state.locationName,
                        localImageUris = state.localImageUris,
                    )
                )
                _uiState.update { it.copy(isSaving = false, savedIncidentId = incident.id) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(savedIncidentId = null) }
    }
}
