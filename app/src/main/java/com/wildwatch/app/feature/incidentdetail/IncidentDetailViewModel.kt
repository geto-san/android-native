package com.wildwatch.app.feature.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.domain.usecase.GetIncidentByIdUseCase
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val distanceKm: Double? = null,
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getIncidentByIdUseCase: GetIncidentByIdUseCase,
) : ViewModel() {
    private val incidentId: String = checkNotNull(savedStateHandle["id"]) // Route param is 'id'

    private val _distanceKm = MutableStateFlow<Double?>(null)

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        getIncidentByIdUseCase(incidentId),
        _distanceKm
    ) { incident, distance ->
        IncidentDetailUiState(incident, distance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentDetailUiState())

    fun loadDistance() {
        // Implement logic
    }
}
