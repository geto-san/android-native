package com.wildwatch.app.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.wildwatch.app.ui.nav.Route
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.location.haversineKm
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentByIdUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val distanceKm: Double? = null,
    val currentUser: User? = null,
) {
    // Community-side conflict/sighting wireframes have no equivalent detail view with
    // actions at all, so Community stays strictly read-only here.
    val isRanger: Boolean get() = currentUser?.role == UserRole.RANGER
    val isAssignedToMe: Boolean get() = incident?.assignedTo != null && incident.assignedTo == currentUser?.uid
    val isAssignedToSomeoneElse: Boolean get() = incident?.assignedTo != null && !isAssignedToMe

    // A ranger can respond (claim + start tracking) to anything not already claimed by
    // someone else and not already resolved - this replaces the old dead-end "Assign to
    // me"/"Start GPS Tracking" two-step: responding now claims the incident as its first
    // effect (see IncidentDetailViewModel.respondToIncident()), so there's only one action.
    val canRespond: Boolean
        get() = isRanger && incident?.status != IncidentStatus.RESOLVED && !isAssignedToSomeoneElse
}

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    getIncidentByIdUseCase: GetIncidentByIdUseCase,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {
    private val incidentId: String = runCatching {
        savedStateHandle.toRoute<Route.IncidentDetail>().id
    }.getOrElse {
        checkNotNull(savedStateHandle["id"]) { "Missing incident id in navigation args" }
    }

    private val _distanceKm = MutableStateFlow<Double?>(null)

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        getIncidentByIdUseCase(incidentId),
        _distanceKm,
        observeUserUseCase(),
    ) { incident, distance, user ->
        IncidentDetailUiState(incident, distance, user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentDetailUiState())

    fun loadDistance() {
        viewModelScope.launch {
            val incident = incidentRepository.getById(incidentId) ?: return@launch
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    _distanceKm.value = haversineKm(
                        lat1 = location.latitude,
                        lng1 = location.longitude,
                        lat2 = incident.lat,
                        lng2 = incident.lng,
                    )
                }
                .onFailure { Timber.w(it, "Unable to resolve distance to incident $incidentId") }
        }
    }

    // Claims the incident on the ranger's behalf the moment they choose to respond, if it
    // isn't already theirs - the UI calls this right before it starts GPS tracking, so
    // "respond" reads as one action instead of a separate assign-then-track sequence.
    fun respondToIncident() {
        if (uiState.value.isAssignedToMe) return
        viewModelScope.launch {
            incidentRepository.assignToSelf(incidentId)
        }
    }
}
