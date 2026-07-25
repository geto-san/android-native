package com.wildwatch.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.User
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
    private val userDataRepository: UserDataRepository,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {

    val currentUser: StateFlow<User?> = observeUserUseCase()

    val shouldShowWelcomeScreen: StateFlow<Boolean> = userDataRepository.shouldShowWelcomeScreen

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

    fun onGoogleSignInClick(context: Context) {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("1015092528863-6uktqq9lfru6blvk4pdomrhbofhdrk1a.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val signInResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = signInResult.exceptionOrNull()?.friendlyMessage())
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Unexpected credential type") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.friendlyMessage()) }
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

    fun dismissWelcomeScreen() {
        userDataRepository.dismissWelcomeScreen()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun Throwable.friendlyMessage(): String = message ?: "Something went wrong. Please try again."
}
