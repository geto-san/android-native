package com.silverback.sentry.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverback.sentry.data.auth.AuthRepository
import com.silverback.sentry.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

// Per guardrail G7, LoginScreen/SignUpScreen only ever call this ViewModel - never
// AuthRepository or FirebaseAuth directly.
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email and password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(email.trim(), password)
            _uiState.update {
                it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.friendlyMessage())
            }
        }
    }

    fun signUp(displayName: String, email: String, password: String) {
        if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(email.trim(), password, displayName.trim())
            _uiState.update {
                it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.friendlyMessage())
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun Throwable.friendlyMessage(): String = message ?: "Something went wrong. Please try again."
}
