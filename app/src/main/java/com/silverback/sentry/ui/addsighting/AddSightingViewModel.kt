package com.silverback.sentry.ui.addsighting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverback.sentry.data.location.LocationRepository
import com.silverback.sentry.data.observation.ObservationRepository
import com.silverback.sentry.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSightingUiState(
    val isLoadingLocation: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val locationError: String? = null,
    val gorillaGroup: String = "",
    val healthStatus: String = "",
    val notes: String = "",
    val photoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedObservationId: String? = null,
) {
    val canSave: Boolean get() = gorillaGroup.isNotBlank() && latitude != null && longitude != null && !isSaving
}

// Per guardrail G7, this ViewModel calls ObservationRepository/LocationRepository
// only - it never touches Room, Firestore, or FusedLocationProviderClient
// directly, which is what makes the tests below possible without a device.
@HiltViewModel
class AddSightingViewModel @Inject constructor(
    private val observationRepository: ObservationRepository,
    private val locationRepository: LocationRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSightingUiState())
    val uiState: StateFlow<AddSightingUiState> = _uiState.asStateFlow()

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

    fun updateGorillaGroup(value: String) = _uiState.update { it.copy(gorillaGroup = value) }

    fun updateHealthStatus(value: String) = _uiState.update { it.copy(healthStatus = value) }

    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun addPhoto(uri: String) = _uiState.update { it.copy(photoUris = it.photoUris + uri) }

    fun removePhoto(uri: String) = _uiState.update {
        it.copy(photoUris = it.photoUris.filterNot { existing -> existing == uri })
    }

    fun save() {
        val state = _uiState.value
        if (state.gorillaGroup.isBlank()) {
            _uiState.update { it.copy(saveError = "Enter the gorilla group name") }
            return
        }
        val latitude = state.latitude
        val longitude = state.longitude
        if (latitude == null || longitude == null) {
            _uiState.update { it.copy(saveError = "Waiting for GPS signal - please try again") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            val observation = observationRepository.create(
                gorillaGroup = state.gorillaGroup.trim(),
                location = "$latitude, $longitude",
                locationName = state.locationName,
                healthStatus = state.healthStatus.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null },
                localImageUris = state.photoUris,
            )
            _uiState.update { it.copy(isSaving = false, savedObservationId = observation.id) }
            // Best-effort nudge - if there's no connectivity right now, WorkManager
            // simply holds this request until its NetworkType.CONNECTED constraint
            // is satisfied, same as the periodic backstop would.
            syncScheduler.triggerImmediateSync()
        }
    }

    fun consumeSavedEvent() = _uiState.update { it.copy(savedObservationId = null) }

    fun clearError() = _uiState.update { it.copy(saveError = null) }
}
