package com.wildwatch.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.auth.AuthRepository
import com.wildwatch.app.data.incident.IncidentRepository
import com.wildwatch.app.data.local.db.IncidentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "Ranger",
    val email: String? = null,
    val resolvedCount: Int = 0,
)

// Per guardrail G7, this ViewModel (not AuthViewModel) owns Profile's own
// AuthRepository/IncidentRepository access - depending on another ViewModel
// from within a ViewModel isn't the right layering. Patrols/Hours stats from
// the wireframe are intentionally omitted rather than fabricated: no
// patrol-session or shift-clock concept exists anywhere in this app's scope.
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    incidentRepository: IncidentRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        incidentRepository.observeAll(),
    ) { user, incidents ->
        ProfileUiState(
            displayName = user?.displayNameOrFallback ?: "Ranger",
            email = user?.email,
            resolvedCount = incidents.count { it.status == IncidentStatus.RESOLVED && it.assignedTo == user?.uid },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun signOut() = authRepository.signOut()
}
