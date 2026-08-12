package com.wildwatch.app.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.incident.NewIncidentDetails
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportIncidentUiState(
    val type: IncidentType = IncidentType.SIGHTING,
    val draftId: String? = null,
    val species: String = "",
    val description: String = "",
    val photos: List<String> = emptyList(),
    val isLocationLoading: Boolean = false,
    val locationName: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val locationError: String? = null,
    val park: Park = Park.BWINDI_IMPENETRABLE,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedIncidentId: String? = null,
) {
    // Species only matters for a wildlife sighting - every other category is fine with
    // just a description. Location isn't part of this: it's auto-captured and allowed to
    // fail gracefully (see locationError) rather than blocking submission entirely, since
    // the whole point of this form is fast, low-friction reporting.
    val canSubmit: Boolean
        get() = description.isNotBlank() && (type != IncidentType.SIGHTING || species.isNotBlank())
}

// Replaces the old remote-schema-driven DynamicReportViewModel/FormEngine subsystem (deleted
// along with this) with a fixed, minimal field set: category, description, optional photos,
// and species (sighting only). GPS/park/timestamp are auto-captured and never shown as fields
// the reporter fills in - see loadCurrentLocation() and IncidentRepositoryImpl.create(), which
// stamps reportedAt itself.
@HiltViewModel
class ReportIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val syncScheduler: SyncScheduler,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportIncidentUiState())
    val uiState: StateFlow<ReportIncidentUiState> = _uiState.asStateFlow()

    private var initializedFor: String? = "unset"

    fun initialize(draftId: String?) {
        // NavHost recomposition can call this more than once for the same screen instance;
        // only actually (re)load state the first time a given draftId (or "new report") is seen.
        if (initializedFor == draftId) return
        initializedFor = draftId

        _uiState.update { ReportIncidentUiState(draftId = draftId) }

        if (draftId != null) {
            viewModelScope.launch {
                incidentRepository.getById(draftId)?.let { incident ->
                    _uiState.update {
                        it.copy(
                            type = incident.type,
                            species = incident.species,
                            description = incident.summary ?: "",
                            photos = incident.localImageUris,
                            park = incident.park,
                        )
                    }
                }
            }
        }

        loadCurrentLocation()
    }

    fun selectType(type: IncidentType) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateSpecies(value: String) {
        _uiState.update { it.copy(species = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun addPhoto(uri: String) {
        _uiState.update { it.copy(photos = it.photos + uri) }
    }

    fun removePhoto(uri: String) {
        _uiState.update { it.copy(photos = it.photos - uri) }
    }

    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocationLoading = true, locationError = null) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val name = locationRepository.reverseGeocode(location.latitude, location.longitude)
                    val park = resolvePark(
                        locationRepository.getParkFromLocation(location.latitude, location.longitude),
                    )
                    _uiState.update {
                        it.copy(
                            lat = location.latitude,
                            lng = location.longitude,
                            locationName = name,
                            park = park,
                            isLocationLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLocationLoading = false, locationError = error.message ?: "Couldn't detect location")
                    }
                }
        }
    }

    fun save(asDraft: Boolean = false) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val details = NewIncidentDetails(
                    type = state.type,
                    park = state.park,
                    community = state.locationName ?: "Unknown",
                    species = if (state.type == IncidentType.SIGHTING) state.species.ifBlank { "Unknown" } else "N/A",
                    severity = IncidentSeverity.MEDIUM,
                    category = null,
                    summary = state.description,
                    lat = state.lat,
                    lng = state.lng,
                    locationName = state.locationName,
                    localImageUris = state.photos,
                )

                val incidentId = if (state.draftId != null) {
                    incidentRepository.update(state.draftId, details, asDraft)
                    state.draftId
                } else {
                    incidentRepository.create(details, asDraft).id
                }

                if (!asDraft) {
                    syncScheduler.triggerImmediateSync()
                    notificationRepository.notifyPendingSync(incidentId)
                }

                _uiState.update { it.copy(isSaving = false, savedIncidentId = incidentId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(savedIncidentId = null) }
    }

    private fun resolvePark(detectedName: String?): Park {
        val normalized = detectedName?.uppercase()?.replace(" ", "_")
        return Park.entries.find { it.name == normalized } ?: Park.BWINDI_IMPENETRABLE
    }
}
