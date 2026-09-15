package com.silversentry.sentry.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.silversentry.sentry.ui.nav.Route
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.silversentry.sentry.core.data.directions.DirectionsRepository
import com.silversentry.sentry.core.data.directions.DrivingRoute
import com.silversentry.sentry.core.data.incident.IncidentRepository
import com.silversentry.sentry.core.data.location.GeoLocation
import com.silversentry.sentry.core.data.location.LocationRepository
import com.silversentry.sentry.core.data.location.haversineKm
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.domain.usecase.GetIncidentByIdUseCase
import com.silversentry.sentry.core.domain.usecase.ObserveUserUseCase
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.model.User
import com.silversentry.sentry.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val distanceKm: Double? = null,
    val currentUser: User? = null,
    val trip: IncidentTripUiState = IncidentTripUiState(),
) {
    // Community-side conflict/sighting wireframes have no equivalent detail view with
    // actions at all, so Community stays strictly read-only here.
    val isRanger: Boolean get() = currentUser?.role == UserRole.RANGER
    val isAssignedToMe: Boolean get() = incident?.assignedTo != null && incident.assignedTo == currentUser?.uid
    val isAssignedToSomeoneElse: Boolean get() = incident?.assignedTo != null && !isAssignedToMe

    // A ranger can respond (claim + start navigation) to anything not already claimed by
    // someone else and not already resolved - this replaces the old dead-end "Assign to
    // me"/"Start GPS Tracking" two-step: responding now claims the incident as its first
    // effect (see IncidentDetailViewModel.respondToIncident()), so there's only one action.
    val canRespond: Boolean
        get() = isRanger && incident?.status != IncidentStatus.RESOLVED && !isAssignedToSomeoneElse
}

// Google-Maps-style trip preview on the incident detail screen: the ranger's live position
// (origin) is resolved on entry, and the driving route to the incident (destination) is
// drawn beneath the "Your location -> Destination" summary. durationSeconds is the Mapbox
// route ETA when the call succeeds; the screen falls back to the haversine distanceKm when
// it fails (offline / no token).
data class IncidentTripUiState(
    val origin: Point? = null,
    val destination: Point? = null,
    val route: DrivingRoute? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val directionsRepository: DirectionsRepository,
    getIncidentByIdUseCase: GetIncidentByIdUseCase,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {
    private val incidentId: String = runCatching {
        savedStateHandle.toRoute<Route.IncidentDetail>().id
    }.getOrElse {
        checkNotNull(savedStateHandle["id"]) { "Missing incident id in navigation args" }
    }

    private val _distanceKm = MutableStateFlow<Double?>(null)
    private val _trip = MutableStateFlow(IncidentTripUiState())

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        getIncidentByIdUseCase(incidentId),
        _distanceKm,
        observeUserUseCase(),
        _trip,
    ) { incident, distance, user, trip ->
        IncidentDetailUiState(incident, distance, user, trip)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentDetailUiState())

    // Resolves both distance measures at once: the haversine straight-line fallback (the
    // old DetailsCard number) and the real driving route that powers the Google-Maps-style
    // "Your location -> Destination" trip preview.
    fun loadTrip() {
        viewModelScope.launch {
            val incident = incidentRepository.getById(incidentId) ?: return@launch
            _trip.update { it.copy(isLoading = true) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    val origin = Point.fromLngLat(location.longitude, location.latitude)
                    val destination = Point.fromLngLat(incident.lng, incident.lat)
                    _distanceKm.value = haversineKm(
                        lat1 = location.latitude,
                        lng1 = location.longitude,
                        lat2 = incident.lat,
                        lng2 = incident.lng,
                    )
                    _trip.update { it.copy(origin = origin, destination = destination) }
                    val originGeo = GeoLocation(location.latitude, location.longitude, location.accuracyMeters)
                    val destinationGeo = GeoLocation(incident.lat, incident.lng, null)
                    directionsRepository.getDrivingRoute(originGeo, destinationGeo)
                        .onSuccess { route ->
                            _trip.update { it.copy(route = route, isLoading = false) }
                        }
                        .onFailure { e ->
                            Timber.w(e, "Unable to resolve driving route to incident $incidentId")
                            _trip.update { it.copy(isLoading = false) }
                        }
                }
                .onFailure { e ->
                    Timber.w(e, "Unable to resolve distance to incident $incidentId")
                    _trip.update { it.copy(isLoading = false) }
                }
        }
    }

    // Claims the incident on the ranger's behalf the moment they choose to respond, if it
    // isn't already theirs - the UI calls this right before it opens navigation, so
    // "respond" reads as one action instead of a separate assign-then-track sequence.
    fun respondToIncident() {
        if (uiState.value.isAssignedToMe) return
        viewModelScope.launch {
            incidentRepository.assignToSelf(incidentId)
        }
    }
}
