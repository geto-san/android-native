package com.silverback.sentry.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverback.sentry.data.connectivity.ConnectivityObserver
import com.silverback.sentry.data.local.db.ObservationStatus
import com.silverback.sentry.data.local.db.SyncStatus
import com.silverback.sentry.data.observation.ObservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val totalCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val attendedCount: Int = 0,
    val needsAttentionCount: Int = 0,
    val isOnline: Boolean = true,
)

// Pure derivation over Phase 4/7's Room-backed observation Flow and Phase 8's
// connectivity Flow (guardrail G7: neither Room nor ConnectivityManager is
// touched directly here). No new data source of its own.
@HiltViewModel
class HomeViewModel @Inject constructor(
    observationRepository: ObservationRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observationRepository.observeAll(),
        connectivityObserver.isOnline,
    ) { observations, online ->
        HomeUiState(
            totalCount = observations.size,
            pendingSyncCount = observations.count { it.syncStatus != SyncStatus.SYNCED },
            attendedCount = observations.count { it.observationStatus == ObservationStatus.ATTENDED },
            needsAttentionCount = observations.count { it.observationStatus != ObservationStatus.ATTENDED },
            isOnline = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
