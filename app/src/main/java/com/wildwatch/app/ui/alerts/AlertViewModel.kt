package com.wildwatch.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.alert.AlertRepository
import com.wildwatch.app.domain.model.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Backs CommunityAlertsScreen with a real AlertRepository.observeAll() Flow -
// see AlertEntity's doc comment for why this is a seeded local table rather
// than a Firestore-synced one.
@HiltViewModel
class AlertViewModel @Inject constructor(
    alertRepository: AlertRepository,
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = alertRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
