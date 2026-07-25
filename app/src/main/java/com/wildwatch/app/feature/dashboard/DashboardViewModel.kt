package com.wildwatch.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val resolvedCount: Int = 0,
    val pendingCount: Int = 0,
    val activeZoneCount: Int = 0,
    val zones: List<String> = emptyList(),
    val selectedZone: String? = null,
    val activeIncidents: List<Incident> = emptyList(),
    val activeAlerts: List<Incident> = emptyList(),
    val isOnline: Boolean = true,
)

// Pure derivation over the Room-backed incident Flow (guardrail G7: no Room/
// Firestore touched directly here). "Active alerts" are unassigned OPEN
// incidents - a filtered view of the same stream, not a separate entity (per
// the domain-pivot's alerts-modeling decision), so assigning one to self is a
// real IncidentRepository mutation, not local-only UI state. "Active
// incidents" are ones already claimed and being worked (IN_PROGRESS) - the
// two lists don't overlap: alerts need an owner, incidents already have one.
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getIncidentsUseCase: GetIncidentsUseCase,
    private val incidentRepository: IncidentRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val selectedZone = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        getIncidentsUseCase(),
        selectedZone,
        connectivityObserver.isOnline,
    ) { incidents, zone, online ->
        val scoped = if (zone == null) incidents else incidents.filter { it.community == zone }
        DashboardUiState(
            resolvedCount = incidents.count { it.status == IncidentStatus.RESOLVED },
            pendingCount = incidents.count { it.status == IncidentStatus.OPEN },
            activeZoneCount = incidents
                .filter { it.status != IncidentStatus.RESOLVED }
                .map { it.community }
                .distinct()
                .size,
            zones = incidents.map { it.community }.distinct().sorted(),
            selectedZone = zone,
            activeIncidents = scoped.filter { it.status == IncidentStatus.IN_PROGRESS },
            activeAlerts = scoped.filter { it.status == IncidentStatus.OPEN && it.assignedTo == null },
            isOnline = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun selectZone(zone: String?) {
        selectedZone.value = zone
    }

    fun assignToSelf(id: String) {
        viewModelScope.launch {
            incidentRepository.assignToSelf(id)
        }
    }
}
