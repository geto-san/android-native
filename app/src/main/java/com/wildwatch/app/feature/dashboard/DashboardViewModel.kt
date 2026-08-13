package com.wildwatch.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.connectivity.ConnectivityObserver
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class DashboardUiState(
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
            zones = incidents.map { it.community }.distinct().sorted(),
            selectedZone = zone,
            activeIncidents = scoped.filter { it.status == IncidentStatus.IN_PROGRESS },
            // Most urgent first (severity), then most recently reported first within a
            // severity tier - previously unsorted (raw query order), which is what users
            // were reacting to when they said the alerts order didn't make sense.
            activeAlerts = scoped
                .filter { it.status == IncidentStatus.OPEN && it.assignedTo == null }
                .sortedWith(
                    compareByDescending<Incident> { it.severity.priority }
                        .thenByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() }
                ),
            isOnline = online,
            unreadNotificationCount = unreadNotifications,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun selectZone(zone: String?) {
        selectedZone.value = zone
    }
}

// Higher value = more urgent. Declared here (not IncidentSeverity's declaration order, which
// is HIGH/LOW/LIGHT/MEDIUM and unusable for priority sorting) since this dashboard's alert
// ordering is the only place severity needs to rank as a priority rather than just a label.
private val IncidentSeverity.priority: Int
    get() = when (this) {
        IncidentSeverity.HIGH -> 3
        IncidentSeverity.MEDIUM -> 2
        IncidentSeverity.LIGHT -> 1
        IncidentSeverity.LOW -> 0
    }
