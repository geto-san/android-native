package com.wildwatch.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.domain.usecase.GetIncidentsUseCase
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String? = null,
    val role: UserRole = UserRole.PUBLIC,
    val isGuest: Boolean = true,
    // null = no explicit preference saved yet - the actually-rendered theme (see
    // MainActivity) falls back to isSystemInDarkTheme() in that case. Left nullable here
    // rather than collapsed to false so the UI layer can resolve it the same way and keep
    // this switch in sync with what's really on screen instead of always showing "off".
    val isDarkTheme: Boolean? = null,
    // Community: incidents this account reported (Incident.userId).
    // Ranger: incidents this account is/was assigned to (Incident.assignedTo).
    // Never the whole table's counts - those aren't "mine" for either role.
    val primaryCount: Int = 0,
    val resolvedCount: Int = 0,
    val zones: List<String> = emptyList(),
)

// wireframe community.profile vs ranger.profile: same shell, different stat
// meaning and a role-specific info section (Ranger: assigned zones; Community:
// none yet - the wireframe's "My badges" gamification row has no backing data
// source, so it's left out rather than faked).
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDataRepository: UserDataRepository,
    observeUserUseCase: ObserveUserUseCase,
    getIncidentsUseCase: GetIncidentsUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        observeUserUseCase(),
        getIncidentsUseCase(),
        userDataRepository.darkThemeConfig
    ) { user, incidents, darkTheme ->
        val role = user?.role ?: UserRole.PUBLIC
        val mine = if (role == UserRole.RANGER) {
            incidents.filter { it.assignedTo == user?.uid }
        } else {
            incidents.filter { it.userId == user?.uid }
        }
        ProfileUiState(
            displayName = user?.displayNameOrFallback ?: "WildWatch User",
            email = user?.email,
            role = role,
            isGuest = user?.isGuest ?: true,
            isDarkTheme = darkTheme,
            primaryCount = mine.size,
            resolvedCount = mine.count { it.status == IncidentStatus.RESOLVED },
            zones = mine.map { it.community }.distinct(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeConfig(enabled)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
