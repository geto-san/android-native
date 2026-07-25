package com.wildwatch.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
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
    val unreadNotificationCount: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getIncidentsUseCase: GetIncidentsUseCase,
    private val incidentRepository: IncidentRepository,
    connectivityObserver: ConnectivityObserver,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    private val selectedZone = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        getIncidentsUseCase(),
        selectedZone,
        connectivityObserver.isOnline,
        notificationRepository.observeUnreadCount(),
    ) { incidents, zone, online, unreadNotifications ->
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
            unreadNotificationCount = unreadNotifications,
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
