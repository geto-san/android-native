package com.wildwatch.app.feature.claims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.claim.ClaimRepository
import com.wildwatch.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ClaimUiState(
    val claims: List<Claim> = emptyList(),
    val filingAsName: String = "",
    val filingAsPark: String = "",
)

@HiltViewModel
class ClaimViewModel @Inject constructor(
    authRepository: AuthRepository,
    claimRepository: ClaimRepository,
) : ViewModel() {
    val uiState: StateFlow<ClaimUiState> = combine(
        claimRepository.observeAll(),
        authRepository.currentUser
    ) { claims, user ->
        ClaimUiState(
            claims = claims,
            filingAsName = user?.displayName ?: "Ranger",
            filingAsPark = "Bwindi Impenetrable"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClaimUiState())
}
