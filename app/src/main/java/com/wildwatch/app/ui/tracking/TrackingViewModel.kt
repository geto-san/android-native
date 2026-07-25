package com.wildwatch.app.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import com.wildwatch.app.data.local.db.Severity
import com.wildwatch.app.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class TrackingFilter {
    ALL,
    URGENT,
    IN_PROGRESS,
    RESOLVED,
    ESCALATED,
}

data class TrackingUiState(
    val incidents: List<Incident> = emptyList(),
    val selectedFilter: TrackingFilter = TrackingFilter.ALL,
)

// Pure derivation over the Room-backed incident Flow, scoped to the current
// ranger's own assignments (guardrail G7: no Room/Firestore touched directly
// here). OPEN incidents never appear here by construction - they're
// unassigned by definition (see the alerts-modeling decision in the domain
// pivot), so this list is always IN_PROGRESS/RESOLVED only.
@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TrackingFilter.ALL)

    val uiState: StateFlow<TrackingUiState> = combine(
        incidentRepository.observeAll(),
        selectedFilter,
    ) { incidents, filter ->
        val myUid = authRepository.currentUser.value?.uid
        val assigned = incidents.filter { it.assignedTo == myUid }
        TrackingUiState(incidents = applyFilter(assigned, filter), selectedFilter = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackingUiState())

    private fun applyFilter(incidents: List<Incident>, filter: TrackingFilter): List<Incident> = when (filter) {
        TrackingFilter.ALL -> incidents
        TrackingFilter.URGENT -> incidents.filter { it.severity == Severity.HIGH || it.severity == Severity.CRITICAL }
        TrackingFilter.IN_PROGRESS -> incidents.filter { it.status == IncidentStatus.IN_PROGRESS }
        TrackingFilter.RESOLVED -> incidents.filter { it.status == IncidentStatus.RESOLVED }
        TrackingFilter.ESCALATED -> incidents.filter { it.isEscalated }
    }

    fun selectFilter(filter: TrackingFilter) {
        selectedFilter.value = filter
    }
}
