package com.silversentry.sentry.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversentry.sentry.core.data.alert.AlertRepository
import com.silversentry.sentry.core.model.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    alertRepository: AlertRepository,
) : ViewModel() {
    val alerts: StateFlow<List<Alert>> = alertRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
