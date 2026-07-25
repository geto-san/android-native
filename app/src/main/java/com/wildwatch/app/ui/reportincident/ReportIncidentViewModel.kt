package com.wildwatch.app.ui.reportincident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.incident.NewIncidentDetails
import com.wildwatch.app.data.local.db.IncidentType
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.data.location.LocationRepository
import com.wildwatch.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportIncidentUiState(
    val isLoadingLocation: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val locationError: String? = null,
    val type: IncidentType = IncidentType.SIGHTING,
    val park: Park = Park.BWINDI_IMPENETRABLE,
    val community: String = "",
    val species: String = "",
    val severity: Severity = Severity.MEDIUM,
    val category: String = "",
    val summary: String = "",
    val photoUris: List<String> = emptyList(),
    // Only used by the conflict-report form's three tagged photo sections
    // (animal / scene / damage); the sighting form only ever uses photoUris.
    val categorizedPhotoUris: Map<String, List<String>> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedIncidentId: String? = null,
) {
    val canSave: Boolean
        get() = species.isNotBlank() && community.isNotBlank() && latitude != null && longitude != null && !isSaving
}

// Per guardrail G7, this ViewModel calls IncidentRepository/LocationRepository
// only - it never touches Room, Firestore, or FusedLocationProviderClient
// directly, which is what makes the tests below possible without a device.
// Voice-note recording is intentionally not built here - voiceNoteUrl stays
// null/unused from this app's own create path (a disproportionate amount of
// UI - recording, playback controls - for a wireframe detail with no product
// signal on priority).
@HiltViewModel
class ReportIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportIncidentUiState())
    val uiState: StateFlow<ReportIncidentUiState> = _uiState.asStateFlow()

    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationName = name,
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Could not get your location"
                    _uiState.update { it.copy(isLoadingLocation = false, locationError = message) }
                }
        }
    }

    fun updateType(value: IncidentType) = _uiState.update { it.copy(type = value) }

    fun updatePark(value: Park) = _uiState.update { it.copy(park = value) }

    fun updateCommunity(value: String) = _uiState.update { it.copy(community = value) }

    fun updateSpecies(value: String) = _uiState.update { it.copy(species = value) }

    fun updateSeverity(value: Severity) = _uiState.update { it.copy(severity = value) }

    fun updateCategory(value: String) = _uiState.update { it.copy(category = value) }

    fun updateSummary(value: String) = _uiState.update { it.copy(summary = value) }

    fun addPhoto(uri: String, category: String? = null) = _uiState.update { state ->
        if (category == null) {
            state.copy(photoUris = state.photoUris + uri)
        } else {
            val updated = state.categorizedPhotoUris.toMutableMap()
            updated[category] = (updated[category] ?: emptyList()) + uri
            state.copy(categorizedPhotoUris = updated)
        }
    }

    fun removePhoto(uri: String, category: String? = null) = _uiState.update { state ->
        if (category == null) {
            state.copy(photoUris = state.photoUris.filterNot { existing -> existing == uri })
        } else {
            val updated = state.categorizedPhotoUris.toMutableMap()
            updated[category] = (updated[category] ?: emptyList()).filterNot { existing -> existing == uri }
            state.copy(categorizedPhotoUris = updated)
        }
    }

    private fun validationError(state: ReportIncidentUiState): String? = when {
        state.species.isBlank() -> "Enter what was observed"
        state.community.isBlank() -> "Enter the zone or community"
        state.latitude == null || state.longitude == null -> "Waiting for GPS signal - please try again"
        else -> null
    }

    fun save() {
        val state = _uiState.value
        val error = validationError(state)
        if (error != null) {
            _uiState.update { it.copy(saveError = error) }
            return
        }
        val latitude = checkNotNull(state.latitude)
        val longitude = checkNotNull(state.longitude)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            val incident = incidentRepository.create(
                NewIncidentDetails(
                    type = state.type,
                    park = state.park,
                    community = state.community.trim(),
                    species = state.species.trim(),
                    severity = state.severity,
                    category = state.category.trim().ifBlank { null },
                    summary = state.summary.trim().ifBlank { null },
                    lat = latitude,
                    lng = longitude,
                    locationName = state.locationName,
                    localImageUris = state.photoUris + state.categorizedPhotoUris.values.flatten(),
                ),
            )
            _uiState.update { it.copy(isSaving = false, savedIncidentId = incident.id) }
            // Best-effort nudge - if there's no connectivity right now, WorkManager
            // simply holds this request until its NetworkType.CONNECTED constraint
            // is satisfied, same as the periodic backstop would.
            syncScheduler.triggerImmediateSync()
        }
    }

    fun consumeSavedEvent() = _uiState.update { it.copy(savedIncidentId = null) }

    fun clearError() = _uiState.update { it.copy(saveError = null) }
}
