package com.wildwatch.app.ui.dashboard

import com.wildwatch.app.data.alert.AlertRepository
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.notification.NotificationRepository
import com.wildwatch.app.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "Ranger",
    val park: String = "Bwindi Impenetrable",
    val language: String = "English",
    val reportsThisMonth: Int = 0,
    val resolvedThisMonth: Int = 0,
    val unreadAlertCount: Int = 0,
    val unreadNotificationCount: Int = 0,
    val recentReports: List<Incident> = emptyList(),
)

// wireframe 5/5b's Home. Community-impact counts, "my recent reports", and
// the unread-alert count are all real, derived from IncidentRepository and
// AlertRepository; the nearby-alert row and live-map card still have no
// backing data source (no location-radius alert query or map-tile feed
// exists), so those two stay static.
@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    incidentRepository: IncidentRepository,
    alertRepository: AlertRepository,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentUser,
        incidentRepository.observeAll(),
        alertRepository.observeAll(),
        notificationRepository.observeUnreadCount(),
    ) { user, incidents, alerts, unreadNotifications ->
        val startOfMonth = Instant.now()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val thisMonth = incidents.filter {
            runCatching { Instant.parse(it.reportedAt) }.getOrNull()?.isAfter(startOfMonth) == true
        }
        HomeUiState(
            displayName = user?.displayNameOrFallback ?: "Ranger",
            reportsThisMonth = thisMonth.size,
            resolvedThisMonth = thisMonth.count { it.status == IncidentStatus.RESOLVED },
            unreadAlertCount = alerts.size,
            unreadNotificationCount = unreadNotifications,
            recentReports = incidents
                .sortedByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() ?: Instant.EPOCH }
                .take(3),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
