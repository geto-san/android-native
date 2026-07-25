package com.wildwatch.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.incident.IncidentRepository
import com.wildwatch.app.core.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MapUiState(
    val incidents: List<Incident> = emptyList(),
)

@HiltViewModel
class MapViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
) : ViewModel() {
    val uiState: StateFlow<MapUiState> = incidentRepository.observeAll()
        .map { MapUiState(incidents = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())
}
