package com.wildwatch.app.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.ui.component.WildWatchLogoMark
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.MagilioFontFamily

// Passwordless by design - see AuthRepository's KDoc. A link only a real inbox owner can
// click is a genuine email-ownership check (unlike a self-reported password), and it
// doubles as sign-up: Firebase creates the account automatically on first successful click,
// so there's no separate sign-up screen/toggle here anymore.
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 40.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WildWatchLogoMark(size = 64.dp)
            Text(
                "WildWatch",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = MagilioFontFamily
                ),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 40.dp)
            )

            when {
                uiState.pendingConfirmationLink != null -> ConfirmEmailView(
                    email = confirmEmail,
                    onEmailChange = { confirmEmail = it },
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onConfirm = { viewModel.confirmEmailForPendingLink(confirmEmail) },
                    onCancel = { viewModel.resetEmailFlow() },
                )

                uiState.emailLinkSent -> CheckYourEmailView(
                    email = email,
                    onUseDifferentEmail = { viewModel.resetEmailFlow() },
                )

                else -> EmailEntryView(
                    email = email,
                    onEmailChange = { email = it },
                    uiState = uiState,
                    onContinueWithEmail = { viewModel.sendEmailSignInLink(email) },
                    onContinueWithGoogle = { viewModel.onGoogleSignInClick(context) },
                    onContinueWithoutAccount = viewModel::signInAnonymously,
                )
            }
        }
    }
}

@Composable
private fun EmailEntryView(
    email: String,
    onEmailChange: (String) -> Unit,
    uiState: AuthUiState,
    onContinueWithEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
) {
    WildWatchTextField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = "Email address",
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { onContinueWithEmail() }),
    )

    Spacer(modifier = Modifier.height(20.dp))

    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    uiState.infoMessage?.let { info ->
        Text(
            text = info,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    Button(
        onClick = onContinueWithEmail,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        enabled = !uiState.isLoading,
    ) {
        if (uiState.loadingAction == AuthLoadingAction.EMAIL) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Continue with email", fontWeight = FontWeight.Bold)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 24.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            "OR",
            style = MaterialTheme.typography.labelMedium,
            color = Grey500,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }

    OutlinedButton(
        onClick = onContinueWithGoogle,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        enabled = !uiState.isLoading,
    ) {
        if (uiState.loadingAction == AuthLoadingAction.GOOGLE) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                "Continue with Google",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onContinueWithoutAccount,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        enabled = !uiState.isLoading,
    ) {
        if (uiState.loadingAction == AuthLoadingAction.ANONYMOUS) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                "Continue without account",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CheckYourEmailView(email: String, onUseDifferentEmail: () -> Unit) {
    Icon(
        Icons.Filled.MarkEmailRead,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        "Check your email",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "We sent a sign-in link to $email. Open it on this device to continue - you don't need a password.",
        style = MaterialTheme.typography.bodyMedium,
        color = Grey500,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = onUseDifferentEmail) {
        Text("Use a different email")
    }
}

@Composable
private fun ConfirmEmailView(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        "Confirm your email",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "This sign-in link wasn't requested from this device. Enter the email address you requested it with to finish signing in.",
        style = MaterialTheme.typography.bodyMedium,
        color = Grey500,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    WildWatchTextField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = "Email address",
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
    )
    Spacer(modifier = Modifier.height(16.dp))
    errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Confirm and sign in", fontWeight = FontWeight.Bold)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onCancel) {
        Text("Cancel")
    }
}
