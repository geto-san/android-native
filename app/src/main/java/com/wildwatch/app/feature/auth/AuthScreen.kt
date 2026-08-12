package com.wildwatch.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.ui.component.WildWatchLogoMark
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.MagilioFontFamily

@Composable
fun AuthScreen(
    startOnSignIn: Boolean,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var signInSelected by remember { mutableStateOf(startOnSignIn) }
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

            AnimatedVisibility(
                visible = !signInSelected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    WildWatchTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full Name",
                        imeAction = ImeAction.Next,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            WildWatchTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                placeholder = "Email address",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            Spacer(modifier = Modifier.height(12.dp))

            WildWatchTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (signInSelected) {
                            viewModel.signIn(emailOrPhone, password)
                        } else {
                            viewModel.signUp(fullName, emailOrPhone, password)
                        }
                    }
                )
            )

            if (signInSelected) {
                TextButton(
                    onClick = { viewModel.sendPasswordReset(emailOrPhone) },
                ) {
                    Text(
                        "Forgot password?",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

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
                onClick = {
                    if (signInSelected) {
                        viewModel.signIn(emailOrPhone, password)
                    } else {
                        viewModel.signUp(fullName, emailOrPhone, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (signInSelected) "Log In" else "Sign Up",
                        fontWeight = FontWeight.Bold
                    )
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
                onClick = { viewModel.onGoogleSignInClick(context) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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
                onClick = viewModel::signInAnonymously,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (signInSelected) "Don't have an account? " else "Already have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey500
                )
                Text(
                    if (signInSelected) "Sign up." else "Log in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        signInSelected = !signInSelected
                        viewModel.clearError()
                    }
                )
            }
        }
    }
}
