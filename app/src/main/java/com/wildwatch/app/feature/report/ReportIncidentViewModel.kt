package com.wildwatch.app.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.Severity
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
    val severity: Severity = Severity.MEDIUM,
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
)

@HiltViewModel
class ReportIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun updateType(type: IncidentType) { _uiState.update { it.copy(type = type) } }
    fun updatePark(park: Park) { _uiState.update { it.copy(park = park) } }
    fun updateCommunity(community: String) { _uiState.update { it.copy(community = community) } }
    fun updateSpecies(species: String) { _uiState.update { it.copy(species = species) } }
    fun updateSeverity(severity: Severity) { _uiState.update { it.copy(severity = severity) } }
    fun updateCategory(category: String?) { _uiState.update { it.copy(category = category) } }
    fun updateSummary(summary: String?) { _uiState.update { it.copy(summary = summary) } }

    fun loadCurrentLocation() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation().onSuccess { location ->
                val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                _uiState.update { 
                    it.copy(
                        lat = location.latitude, 
                        lng = location.longitude,
                        locationName = name
                    ) 
                }
            }
        }
    }

    fun addPhoto(uri: String, category: String? = null) {
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
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val incident = incidentRepository.create(
                    NewIncidentDetails(
                        type = state.type,
                        park = state.park,
                        community = state.community,
                        species = state.species,
                        severity = state.severity,
                        category = state.category,
                        summary = state.summary,
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
