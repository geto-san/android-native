package com.wildwatch.app.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.ui.component.WildWatchLogoMark
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Grey500

@Composable
fun AuthScreen(
    startOnSignIn: Boolean,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var signInSelected by remember { mutableStateOf(startOnSignIn) }
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("ranger@wildwatch.com") }
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
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 40.dp)
            )

            if (!signInSelected) {
                WildWatchTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "Full Name",
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            WildWatchTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                placeholder = "Email address",
                keyboardType = KeyboardType.Email,
            )
            Spacer(modifier = Modifier.height(12.dp))

            WildWatchTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )

            if (signInSelected) {
                TextButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.End),
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
                Text(
                    if (signInSelected) "Log In" else "Sign Up",
                    fontWeight = FontWeight.Bold
                )
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

            TextButton(onClick = {}) {
                Text(
                    "Continue with Google",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
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
