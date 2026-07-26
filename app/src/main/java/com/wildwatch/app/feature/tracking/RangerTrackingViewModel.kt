package com.wildwatch.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.Severity
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class TrackingFilter(val label: String) {
    ALL("All"),
    URGENT("Urgent"),
    IN_PROGRESS("In progress"),
    RESOLVED("Resolved"),
    ESCALATED("Escalated"),
}

data class RangerTrackingUiState(
    val resolvedCount: Int = 0,
    val inProgressCount: Int = 0,
    val escalatedCount: Int = 0,
    val selectedFilter: TrackingFilter = TrackingFilter.ALL,
    val cases: List<Incident> = emptyList(),
)

// wireframe ranger.tracking - "Your assigned cases": distinct from
// ranger.index/DashboardScreen's live triage view (new incidents + unassigned
// alerts). This is the ranger's own case log across every lifecycle status,
// so it's scoped to Incident.assignedTo == currentUser.uid, same scoping
// rule as ProfileViewModel uses for its "Assigned" stat.
@HiltViewModel
class RangerTrackingViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TrackingFilter.ALL)

    val uiState: StateFlow<RangerTrackingUiState> = combine(
        incidentRepository.observeAll(),
        observeUserUseCase(),
        selectedFilter,
    ) { incidents, user, filter ->
        val myCases = incidents.filter { it.assignedTo == user?.uid }
        val filtered = when (filter) {
            TrackingFilter.ALL -> myCases
            TrackingFilter.URGENT -> myCases.filter { it.severity == Severity.CRITICAL }
            TrackingFilter.IN_PROGRESS -> myCases.filter { it.status == IncidentStatus.IN_PROGRESS }
            TrackingFilter.RESOLVED -> myCases.filter { it.status == IncidentStatus.RESOLVED }
            TrackingFilter.ESCALATED -> myCases.filter { it.isEscalated }
        }
        RangerTrackingUiState(
            resolvedCount = myCases.count { it.status == IncidentStatus.RESOLVED },
            inProgressCount = myCases.count { it.status == IncidentStatus.IN_PROGRESS },
            escalatedCount = myCases.count { it.isEscalated },
            selectedFilter = filter,
            cases = filtered.sortedByDescending { it.lastModified },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RangerTrackingUiState())

    fun selectFilter(filter: TrackingFilter) {
        selectedFilter.value = filter
    }
}
