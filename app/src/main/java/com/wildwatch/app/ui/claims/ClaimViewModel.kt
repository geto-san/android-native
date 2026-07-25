package com.wildwatch.app.ui.claims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.claim.ClaimRepository
import com.wildwatch.app.domain.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ClaimUiState(
    val filingAsName: String = "Ranger",
    val filingAsPark: String = "Bwindi Impenetrable",
    val claims: List<Claim> = emptyList(),
)

// Backs CompensationClaimScreen's real "My claims" list - a genuine
// ClaimRepository.observeAllForCurrentUser() Flow, not the hardcoded MyClaim
// list this screen used to render.
@HiltViewModel
class ClaimViewModel @Inject constructor(
    authRepository: AuthRepository,
    claimRepository: ClaimRepository,
) : ViewModel() {

    val uiState: StateFlow<ClaimUiState> = combine(
        authRepository.currentUser,
        claimRepository.observeAllForCurrentUser(),
    ) { user, claims ->
        ClaimUiState(
            filingAsName = user?.displayNameOrFallback ?: "Ranger",
            claims = claims,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClaimUiState())
}
