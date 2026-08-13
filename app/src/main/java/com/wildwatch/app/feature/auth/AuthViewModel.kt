package com.wildwatch.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.wildwatch.app.core.data.auth.AuthErrorMapper
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.domain.usecase.ObserveUserUseCase
import com.wildwatch.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Which of the three Auth screen buttons (if any) is currently in flight - lets the UI show
// a spinner on only the button that was actually tapped instead of all three at once.
enum class AuthLoadingAction { EMAIL, GOOGLE, ANONYMOUS }

data class AuthUiState(
    val loadingAction: AuthLoadingAction? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    // True once sendEmailSignInLink() succeeds - AuthScreen swaps to a "check your email"
    // view instead of the email field, since there's nothing left to fill in.
    val emailLinkSent: Boolean = false,
    // Set when completeEmailLinkSignIn() is called but no address was saved locally to pair
    // with the link (opened on a different device, or storage was cleared) - AuthScreen
    // renders a one-field "confirm your email" prompt instead of silently failing.
    val pendingConfirmationLink: String? = null,
) {
    val isLoading: Boolean get() = loadingAction != null
}

// Per guardrail G7, AuthScreen only ever calls this ViewModel - never AuthRepository or
// FirebaseAuth directly. Email sign-in is passwordless (Firebase email-link) by design: see
// AuthRepository's KDoc for why, and MainActivity for how a clicked link reaches
// completeEmailLinkSignIn().
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDataRepository: UserDataRepository,
    observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {

    val currentUser: StateFlow<User?> = observeUserUseCase()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun sendEmailSignInLink(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email address") }
            return
        }
        val trimmedEmail = email.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(loadingAction = AuthLoadingAction.EMAIL, errorMessage = null, infoMessage = null) }
            val result = authRepository.sendSignInLinkToEmail(trimmedEmail)
            if (result.isSuccess) {
                userDataRepository.setPendingEmailLinkAddress(trimmedEmail)
            }
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(loadingAction = null, emailLinkSent = true)
                } else {
                    it.copy(loadingAction = null, errorMessage = result.exceptionOrNull()?.let(AuthErrorMapper::messageForSendEmailLink))
                }
            }
        }
    }

    fun isEmailSignInLink(link: String): Boolean = authRepository.isSignInWithEmailLink(link)

    /** Called (from MainActivity) once the user has actually tapped the emailed link. */
    fun completeEmailLinkSignIn(link: String) {
        if (!authRepository.isSignInWithEmailLink(link)) return
        viewModelScope.launch {
            val savedEmail = userDataRepository.pendingEmailLinkAddress.first()
            if (savedEmail == null) {
                _uiState.update { it.copy(pendingConfirmationLink = link, emailLinkSent = false) }
                return@launch
            }
            finishEmailLinkSignIn(savedEmail, link)
        }
    }

    /** The fallback path: no saved address matched this link, so the user typed one in. */
    fun confirmEmailForPendingLink(email: String) {
        val link = _uiState.value.pendingConfirmationLink ?: return
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter the email address this link was sent to") }
            return
        }
        viewModelScope.launch { finishEmailLinkSignIn(email.trim(), link) }
    }

    private suspend fun finishEmailLinkSignIn(email: String, link: String) {
        _uiState.update { it.copy(loadingAction = AuthLoadingAction.EMAIL, errorMessage = null) }
        val result = authRepository.signInWithEmailLink(email, link)
        if (result.isSuccess) {
            userDataRepository.setPendingEmailLinkAddress(null)
        }
        _uiState.update {
            it.copy(
                loadingAction = null,
                pendingConfirmationLink = if (result.isSuccess) null else it.pendingConfirmationLink,
                emailLinkSent = false,
                errorMessage = result.exceptionOrNull()?.let(AuthErrorMapper::messageForCompleteEmailLink),
            )
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingAction = AuthLoadingAction.ANONYMOUS, errorMessage = null) }
            val result = authRepository.signInAnonymously()
            _uiState.update {
                it.copy(loadingAction = null, errorMessage = result.exceptionOrNull()?.friendlyMessage())
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
            _uiState.update { it.copy(loadingAction = AuthLoadingAction.GOOGLE, errorMessage = null) }
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val signInResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                    _uiState.update {
                        it.copy(loadingAction = null, errorMessage = signInResult.exceptionOrNull()?.friendlyMessage())
                    }
                } else {
                    _uiState.update { it.copy(loadingAction = null, errorMessage = "Unexpected credential type") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingAction = null, errorMessage = e.friendlyMessage()) }
            }
        }
    }

    /** Backs out of either the "check your email" or "confirm your email" view. */
    fun resetEmailFlow() {
        _uiState.update { it.copy(emailLinkSent = false, pendingConfirmationLink = null, errorMessage = null) }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun Throwable.friendlyMessage(): String = message ?: "Something went wrong. Please try again."
}
