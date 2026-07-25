package com.wildwatch.app.feature.claims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.claim.ClaimRepository
import com.wildwatch.app.core.model.Claim
import com.wildwatch.app.core.database.ClaimCategory
import com.wildwatch.app.core.database.ClaimStatus
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.database.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NewClaimUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
)

@HiltViewModel
class NewClaimViewModel @Inject constructor(
    private val claimRepository: ClaimRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewClaimUiState())
    val uiState: StateFlow<NewClaimUiState> = _uiState.asStateFlow()

    fun submitClaim(
        category: ClaimCategory,
        park: Park,
        description: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val claim = Claim(
                    id = UUID.randomUUID().toString(),
                    category = category,
                    status = ClaimStatus.UNDER_VERIFICATION,
                    park = park,
                    description = description,
                    lat = null,
                    lng = null,
                    locationName = null,
                    userName = null,
                    userEmail = null,
                    userId = null,
                    filedAt = java.time.Instant.now().toString(),
                    syncStatus = SyncStatus.PENDING,
                    lastModified = System.currentTimeMillis()
                )
                claimRepository.create(claim)
                _uiState.update { it.copy(isSaving = false, isSubmitted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
