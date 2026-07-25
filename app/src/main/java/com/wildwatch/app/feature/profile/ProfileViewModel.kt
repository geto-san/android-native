package com.wildwatch.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.database.IncidentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String? = null,
    val resolvedCount: Int = 0,
    val incidentCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeUserUseCase: ObserveUserUseCase,
    getIncidentsUseCase: GetIncidentsUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        observeUserUseCase(),
        getIncidentsUseCase()
    ) { user, incidents ->
        ProfileUiState(
            displayName = user?.displayName ?: "WildWatch User",
            email = user?.email,
            resolvedCount = incidents.count { it.status == IncidentStatus.RESOLVED },
            incidentCount = incidents.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun signOut() {
        authRepository.signOut()
    }
}
