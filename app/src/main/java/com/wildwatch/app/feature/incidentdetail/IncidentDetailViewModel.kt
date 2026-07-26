package com.wildwatch.app.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.domain.usecase.GetIncidentByIdUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.Incident
import com.wildwatch.app.core.model.User
import com.wildwatch.app.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val distanceKm: Double? = null,
    val currentUser: User? = null,
) {
    // wireframe ranger.incident has Call/Message/Start GPS actions; the
    // community-side conflict/sighting wireframes have no equivalent detail
    // view with actions at all, so Community stays strictly read-only here.
    val isRanger: Boolean get() = currentUser?.role == UserRole.RANGER
    val isAssignedToMe: Boolean get() = incident?.assignedTo != null && incident.assignedTo == currentUser?.uid
    val canAssignToSelf: Boolean get() = isRanger && incident?.assignedTo == null
}

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    getIncidentByIdUseCase: GetIncidentByIdUseCase,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {
    private val incidentId: String = checkNotNull(savedStateHandle["id"]) // Route param is 'id'

    private val _distanceKm = MutableStateFlow<Double?>(null)

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        getIncidentByIdUseCase(incidentId),
        _distanceKm,
        observeUserUseCase(),
    ) { incident, distance, user ->
        IncidentDetailUiState(incident, distance, user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentDetailUiState())

    fun loadDistance() {
        // Implement logic
    }

    fun assignToSelf() {
        viewModelScope.launch {
            incidentRepository.assignToSelf(incidentId)
        }
    }
}
