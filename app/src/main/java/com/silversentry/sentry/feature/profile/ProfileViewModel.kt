package com.silversentry.sentry.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversentry.sentry.core.data.auth.AuthRepository
import com.silversentry.sentry.core.data.user.UserDataRepository
import com.silversentry.sentry.core.data.wipe.LocalDataClearer
import com.silversentry.sentry.core.domain.usecase.GetIncidentsUseCase
import com.silversentry.sentry.core.domain.usecase.ObserveUserUseCase
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.Park
import com.silversentry.sentry.core.model.UserRole
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
    // The account's park, resolved from the Firestore slug in custom claims to a
    // readable name. Null when the account has no park claim (e.g. a public user).
    val parkName: String? = null,
)

// wireframe community.profile vs ranger.profile: same shell, different stat
// meaning and a role-specific info section (Ranger: assigned zones; Community:
// none yet - the wireframe's "My badges" gamification row has no backing data
// source, so it's left out rather than faked).
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDataRepository: UserDataRepository,
    private val localDataClearer: LocalDataClearer,
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
            displayName = user?.displayNameOrFallback ?: "SilverBack Sentry User",
            email = user?.email,
            role = role,
            isGuest = user?.isGuest ?: true,
            isDarkTheme = darkTheme,
            primaryCount = mine.size,
            resolvedCount = mine.count { it.status == IncidentStatus.RESOLVED },
            zones = mine.map { it.community }.distinct(),
            parkName = Park.fromFirestoreId(user?.parkId)?.displayName,
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

    // Clears every piece of on-device state (Room + DataStore) while keeping the current
    // Firebase sign-in session - Firestore re-fills the offline cache through its listeners.
    fun clearLocalData() {
        viewModelScope.launch {
            localDataClearer.clearAllLocalData()
        }
    }
}
