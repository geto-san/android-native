package com.wildwatch.app.feature.dashboard

import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Article
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "Ranger",
    val park: String = "Bwindi Impenetrable",
    val language: String = "English",
    val reportsThisMonth: Int = 0,
    val resolvedThisMonth: Int = 0,
    val unreadNotificationCount: Int = 0,
    val recentReports: List<Incident> = emptyList(),
    val drafts: List<Incident> = emptyList(),
    val pendingUploadsCount: Int = 0,
    val zones: List<String> = emptyList(),
    val communityAlerts: List<Incident> = emptyList(),
    val newsArticles: List<Article> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val incidentRepository: com.wildwatch.app.core.data.incident.IncidentRepository,
    private val articleRepository: ArticleRepository,
    observeUserUseCase: ObserveUserUseCase,
    getIncidentsUseCase: GetIncidentsUseCase,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeUserUseCase(),
        getIncidentsUseCase(),
        notificationRepository.observeUnreadCount(),
        articleRepository.observeAll()
    ) { user, incidents, unreadNotifications, articles ->
        val startOfMonth = Instant.now()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val thisMonth = incidents.filter {
            runCatching { Instant.parse(it.reportedAt) }.getOrNull()?.isAfter(startOfMonth) == true
        }
        
        val drafts = incidents.filter { it.syncStatus == SyncStatus.DRAFT }
        val pendingCount = incidents.count { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.PENDING_UPDATE }
        val syncedReports = incidents.filter { it.syncStatus != SyncStatus.DRAFT }

        val twentyFourHoursAgo = Instant.now().minusSeconds(24 * 60 * 60)
        val communityAlerts = syncedReports.filter {
            val isAlertableType = it.type == com.wildwatch.app.core.database.IncidentType.SIGHTING ||
                    it.type == com.wildwatch.app.core.database.IncidentType.CONFLICT ||
                    it.type == com.wildwatch.app.core.database.IncidentType.EMERGENCY
            val isRecent = runCatching { Instant.parse(it.reportedAt) }.getOrNull()?.isAfter(twentyFourHoursAgo) == true
            val isUnresolved = it.status != IncidentStatus.RESOLVED
            isAlertableType && isRecent && isUnresolved
        }.sortedByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() ?: Instant.EPOCH }

        HomeUiState(
            displayName = user?.displayNameOrFallback ?: "Ranger",
            reportsThisMonth = thisMonth.size,
            resolvedThisMonth = thisMonth.count { it.status == IncidentStatus.RESOLVED },
            unreadNotificationCount = unreadNotifications,
            recentReports = syncedReports
                .sortedByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() ?: Instant.EPOCH }
                .take(5), // Reduced for home screen
            drafts = drafts,
            pendingUploadsCount = pendingCount,
            zones = syncedReports.map { it.community }.distinct().sorted(),
            communityAlerts = communityAlerts,
            newsArticles = articles.take(3) // Only a few for home screen
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun triggerSync() {
        viewModelScope.launch {
            incidentRepository.syncPending()
        }
    }
}
