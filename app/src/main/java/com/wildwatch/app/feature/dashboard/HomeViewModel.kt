package com.wildwatch.app.feature.dashboard

import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.data.notification.NotificationRepository
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.SyncStatus
import com.wildwatch.app.core.model.Article
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.User
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
    val currentUserId: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val incidentRepository: com.wildwatch.app.core.data.incident.IncidentRepository,
    private val articleRepository: ArticleRepository,
    observeUserUseCase: ObserveUserUseCase,
    getIncidentsUseCase: GetIncidentsUseCase,
    notificationRepository: NotificationRepository,
    private val alertPreferences: CommunityAlertPreferences,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            observeUserUseCase(),
            getIncidentsUseCase(),
            notificationRepository.observeUnreadCount(),
            articleRepository.observeAll(),
        ) { user, incidents, unreadNotifications, articles ->
            CoreHomeInputs(user, incidents, unreadNotifications, articles)
        },
        alertPreferences.dismissedAlertIds,
        alertPreferences.seenAlertTimestamps,
    ) { core, dismissedIds, seenTimestamps ->
        buildHomeState(core, dismissedIds, seenTimestamps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun markAlertSeen(alertId: String) {
        viewModelScope.launch { alertPreferences.markSeen(alertId) }
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch { alertPreferences.dismiss(alertId) }
    }

    private fun buildHomeState(
        core: CoreHomeInputs,
        dismissedIds: Set<String>,
        seenTimestamps: Map<String, Long>,
    ): HomeUiState {
        val user = core.user
        val incidents = core.incidents
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
        val pendingCount = incidents.count {
            it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.PENDING_UPDATE
        }
        val syncedReports = incidents.filter { it.syncStatus != SyncStatus.DRAFT }

        val communityAlerts = syncedReports.filter { incident ->
            CommunityAlertFilter.shouldShow(incident, user?.uid, dismissedIds, seenTimestamps)
        }.sortedByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() ?: Instant.EPOCH }

        return HomeUiState(
            displayName = user?.displayNameOrFallback ?: "Ranger",
            reportsThisMonth = thisMonth.size,
            resolvedThisMonth = thisMonth.count { it.status == IncidentStatus.RESOLVED },
            unreadNotificationCount = core.unreadNotifications,
            recentReports = syncedReports
                .sortedByDescending { runCatching { Instant.parse(it.reportedAt) }.getOrNull() ?: Instant.EPOCH }
                .take(5),
            drafts = drafts,
            pendingUploadsCount = pendingCount,
            zones = syncedReports.map { it.community }.distinct().sorted(),
            communityAlerts = communityAlerts,
            newsArticles = core.articles.take(3),
            currentUserId = user?.uid,
        )
    }

    private data class CoreHomeInputs(
        val user: User?,
        val incidents: List<Incident>,
        val unreadNotifications: Int,
        val articles: List<Article>,
    )
}
