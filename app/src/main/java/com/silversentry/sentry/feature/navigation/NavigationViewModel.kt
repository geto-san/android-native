package com.silversentry.sentry.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mapbox.geojson.Point
import com.silversentry.sentry.core.data.directions.DirectionsRepository
import com.silversentry.sentry.core.data.incident.IncidentRepository
import com.silversentry.sentry.core.data.location.GeoLocation
import com.silversentry.sentry.core.data.location.LocationRepository
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.ui.component.displayTitle
import com.silversentry.sentry.ui.nav.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class NavigationUiState(
    val incident: Incident? = null,
    val userLocation: Point? = null,
    val destination: Point? = null,
    val routePoints: List<Point> = emptyList(),
    val distanceKm: Double? = null,
    val durationSeconds: Double? = null,
    val isFollowing: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasValidDestination: Boolean
        get() = destination != null
    val destinationName: String
        get() = incident?.displayTitle() ?: "Destination"
    val destinationPlace: String
        get() = incident?.locationName ?: incident?.community ?: ""
}

// Full-screen Google-Maps-style navigation to the incident. On entry the incident is
// claimed by this ranger (assignToSelf is idempotent), then the current GPS position is
// polled on a fixed cadence - a one-shot getCurrentLocation per tick, intentionally not a
// continuous stream, which stays inside the app's location surface rules (see
// LocationRepository) - and the driving route is re-fetched so the line and ETA track the
// ranger as they move. Stops cleanly via stopFollowing() (patrol service stopped by the
// screen).
@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val directionsRepository: DirectionsRepository,
) : ViewModel() {

    private val incidentId: String = runCatching {
        savedStateHandle.toRoute<Route.Navigation>().incidentId
    }.getOrElse {
        checkNotNull(savedStateHandle["incidentId"]) { "Missing incident id in navigation args" }
    }

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var followingJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { incidentRepository.assignToSelf(incidentId) }
                .onFailure { Timber.w(it, "Could not claim incident $incidentId while navigating") }

            val incident = incidentRepository.getById(incidentId)
            if (incident != null) {
                val destination = if (incident.lat != 0.0 || incident.lng != 0.0) {
                    Point.fromLngLat(incident.lng, incident.lat)
                } else {
                    null
                }
                _uiState.update { it.copy(incident = incident, destination = destination) }
                if (destination != null) refresh()
            }
        }
    }

    // Starts (or resumes) the follow loop: refresh GPS, redraw the route to the incident
    // and recompute the ETA. Safe to call repeatedly - only one loop runs.
    fun startFollowing() {
        if (followingJob?.isActive == true) return
        _uiState.update { it.copy(isFollowing = true) }
        followingJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(FOLLOW_INTERVAL_MS)
            }
        }
    }

    fun stopFollowing() {
        followingJob?.cancel()
        followingJob = null
        _uiState.update { it.copy(isFollowing = false) }
    }

    private suspend fun refresh() {
        val destination = _uiState.value.destination ?: return

        locationRepository.getCurrentLocation()
            .onSuccess { location ->
                val origin = Point.fromLngLat(location.longitude, location.latitude)
                _uiState.update { it.copy(userLocation = origin, errorMessage = null) }
                directionsRepository.getDrivingRoute(
                    origin = GeoLocation(location.latitude, location.longitude, location.accuracyMeters),
                    destination = GeoLocation(destination.latitude(), destination.longitude(), null),
                )
                    .onSuccess { route ->
                        _uiState.update {
                            it.copy(
                                routePoints = route.points,
                                distanceKm = route.distanceMeters / 1000.0,
                                durationSeconds = route.durationSeconds,
                            )
                        }
                    }
                    .onFailure { e ->
                        Timber.w(e, "Route refresh to $incidentId failed")
                        _uiState.update { it.copy(errorMessage = "Route unavailable") }
                    }
            }
            .onFailure { e ->
                Timber.w(e, "Location refresh during navigation to $incidentId failed")
                _uiState.update { it.copy(errorMessage = "Location unavailable") }
            }
    }

    override fun onCleared() {
        stopFollowing()
        super.onCleared()
    }

    private companion object {
        const val FOLLOW_INTERVAL_MS = 6000L
    }
}
