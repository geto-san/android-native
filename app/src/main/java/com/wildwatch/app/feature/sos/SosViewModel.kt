package com.wildwatch.app.feature.sos

import androidx.lifecycle.ViewModel
import com.wildwatch.app.core.domain.usecase.TriggerSosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SosUiState(
    val isSending: Boolean = false,
    val isSent: Boolean = false
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val triggerSosUseCase: TriggerSosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun triggerSos() {
        _uiState.update { it.copy(isSending = true) }
        triggerSosUseCase()
        _uiState.update { it.copy(isSending = false, isSent = true) }
    }
    
    fun reset() {
        _uiState.update { it.copy(isSending = false, isSent = false) }
    }
}
