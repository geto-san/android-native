package com.wildwatch.app.ui.claims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.claim.ClaimRepository
import com.wildwatch.app.data.claim.NewClaimDetails
import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.data.local.db.Park
import com.wildwatch.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewClaimUiState(
    val description: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedClaimId: String? = null,
)

// Backs NewClaimScreen: a real ClaimRepository.create() call (Room-first,
// guardrail G2), not a form that discards its input.
@HiltViewModel
class NewClaimViewModel @Inject constructor(
    private val claimRepository: ClaimRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewClaimUiState())
    val uiState: StateFlow<NewClaimUiState> = _uiState.asStateFlow()

    fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }

    fun submit(category: ClaimCategory) {
        val state = _uiState.value
        if (state.description.isBlank()) {
            _uiState.update { it.copy(saveError = "Describe what happened before submitting") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            val claim = claimRepository.create(
                NewClaimDetails(
                    category = category,
                    park = Park.BWINDI_IMPENETRABLE,
                    description = state.description.trim(),
                    lat = null,
                    lng = null,
                    locationName = null,
                    relatedIncidentId = null,
                    localImageUris = emptyList(),
                ),
            )
            _uiState.update { it.copy(isSaving = false, savedClaimId = claim.id) }
            syncScheduler.triggerImmediateClaimSync()
        }
    }

    fun consumeSavedEvent() = _uiState.update { it.copy(savedClaimId = null) }
}
