package com.wildwatch.app.ui.auth

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.ui.components.GradientHeader
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.PillOutlineButtonRow
import com.wildwatch.app.ui.components.TwoOptionSegmentedControl
import com.wildwatch.app.ui.components.WildWatchLogoMark
import com.wildwatch.app.ui.components.WildWatchTextField
import com.wildwatch.app.ui.theme.Cream

private val LANGUAGES = listOf("English", "Swahili", "Luganda", "Runyankole")
private val PARKS = listOf(
    "Bwindi Impenetrable",
    "Mgahinga Gorilla",
    "Murchison Falls",
    "Queen Elizabeth",
    "Kibale",
)

// wireframes 2 & 3: a single screen with a Sign In/Register pill toggle
// rather than two separate screens, matching the wireframes exactly.
@Composable
fun AuthScreen(
    startOnSignIn: Boolean,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var signInSelected by remember { mutableStateOf(startOnSignIn) }
    var emailModeSelected by remember { mutableStateOf(true) }
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(LANGUAGES.first()) }
    var park by remember { mutableStateOf(PARKS.first()) }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GradientHeader {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WildWatchLogoMark(size = 40.dp)
                        Text(
                            "Wildwatch",
                            style = MaterialTheme.typography.titleLarge,
                            color = Cream,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Text(
                        text = if (signInSelected) "Welcome back" else "Create your account",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Cream,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = if (signInSelected) {
                            "Sign in to continue protecting wildlife."
                        } else {
                            "Join the conservation community."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cream.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-12).dp)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TwoOptionSegmentedControl(
                        firstLabel = "Sign In",
                        secondLabel = "Register",
                        firstSelected = signInSelected,
                        onSelectFirst = { signInSelected = true; viewModel.clearError() },
                        onSelectSecond = { signInSelected = false; viewModel.clearError() },
                        trackColor = MaterialTheme.colorScheme.surface,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TwoOptionSegmentedControl(
                    firstLabel = "Email",
                    secondLabel = "Phone",
                    firstSelected = emailModeSelected,
                    onSelectFirst = { emailModeSelected = true },
                    onSelectSecond = { emailModeSelected = false },
                    firstIcon = Icons.Filled.Email,
                    secondIcon = Icons.Filled.Phone,
                    optionVerticalPadding = 8.dp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!signInSelected) {
                    WildWatchTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full name",
                        leadingIcon = Icons.Filled.Person,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                WildWatchTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    placeholder = if (emailModeSelected) "you@example.com" else "+256 700 000 000",
                    leadingIcon = if (emailModeSelected) Icons.Filled.Email else Icons.Filled.Phone,
                    keyboardType = if (emailModeSelected) KeyboardType.Email else KeyboardType.Phone,
                )
                Spacer(modifier = Modifier.height(12.dp))

                WildWatchTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                )

                if (signInSelected) {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot password?", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "PREFERRED LANGUAGE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    com.wildwatch.app.ui.components.WildWatchDropdownField(
                        value = language,
                        options = LANGUAGES,
                        onSelect = { language = it },
                        displayName = { it },
                        leadingIcon = Icons.Filled.Language,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "NATIONAL PARK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    com.wildwatch.app.ui.components.WildWatchDropdownField(
                        value = park,
                        options = PARKS,
                        onSelect = { park = it },
                        displayName = { it },
                        leadingIcon = Icons.Filled.Park,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PillButton(
                    text = if (signInSelected) "Sign in" else "Create account",
                    loading = uiState.isLoading,
                    onClick = {
                        if (signInSelected) {
                            viewModel.signIn(emailOrPhone, password)
                        } else {
                            viewModel.signUp(fullName, emailOrPhone, password)
                        }
                    },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 20.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "or continue with",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                PillOutlineButtonRow(
                    firstText = "Google",
                    onFirstClick = {},
                    secondText = "Apple",
                    onSecondClick = {},
                )

                if (signInSelected) {
                    Text(
                        text = "By continuing you agree to our Terms and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
