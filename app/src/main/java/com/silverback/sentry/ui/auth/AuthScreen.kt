package com.silverback.sentry.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silverback.sentry.ui.components.WildwatchGradients
import com.silverback.sentry.ui.theme.SilverBackSentryTheme
import kotlinx.coroutines.launch

/**
 * Screen 2/18 — Auth: Login + Register combined (source: wildwatch.zip
 * `src/routes/auth.tsx`). The wireframe uses one screen with a segmented
 * Sign in/Register control rather than two separate routes, so this replaces
 * both the old LoginScreen and SignUpScreen composables and Route.Login /
 * Route.SignUp collapse into a single Route.Auth (see SentryNavHost).
 *
 * Backend scope note (see PLAN.md §2/§5): the wireframe also offers phone
 * sign-in and Google/Apple social buttons, and captures preferred
 * language + national park on registration. None of that is wired to a real
 * backend yet - phone/social auth aren't configured in Firebase, and
 * language/park have nowhere to persist until the `users.role` /
 * profile-fields decision in PLAN.md §5 is made. Those controls are rendered
 * per the design (so the screen matches pixel-for-pixel) but surface a
 * "coming soon" / are captured only in local Composable state for now,
 * clearly commented at each spot below.
 */
private enum class AuthMode { Login, Register }
private enum class AuthMethod { Email, Phone }

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.Login) }
    var method by remember { mutableStateOf(AuthMethod.Email) }
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Captured locally only - see backend scope note above. Not sent anywhere yet.
    var language by remember { mutableStateOf("English") }
    var park by remember { mutableStateOf("Bwindi Impenetrable") }

    fun notImplemented(feature: String) = scope.launch {
        snackbarHostState.showSnackbar("$feature isn't available yet")
    }

    fun submit() {
        if (method == AuthMethod.Phone) {
            notImplemented("Phone sign-in")
            return
        }
        if (mode == AuthMode.Login) {
            viewModel.signIn(emailOrPhone, password)
        } else {
            // language/park aren't passed to signUp() yet - AuthViewModel.signUp
            // only persists displayName/email/password today (see PLAN.md §5).
            viewModel.signUp(fullName, emailOrPhone, password)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Forest-gradient header band, rounded at the bottom (wireframe's
                // gradient-forest ... rounded-b-[36px]).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WildwatchGradients.forest(), RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Park, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Wildwatch", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text(
                        text = if (mode == AuthMode.Login) "Welcome back" else "Create your account",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = if (mode == AuthMode.Login) {
                            "Sign in to continue protecting wildlife."
                        } else {
                            "Join the conservation community."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Login/Register segmented tab, overlapping the header band.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-20).dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                    ) {
                        SegmentButton("Sign in", mode == AuthMode.Login, Modifier.weight(1f)) { mode = AuthMode.Login }
                        SegmentButton("Register", mode == AuthMode.Register, Modifier.weight(1f)) { mode = AuthMode.Register }
                    }

                    // Email/Phone method toggle.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                    ) {
                        MethodButton("Email", Icons.Filled.Mail, method == AuthMethod.Email, Modifier.weight(1f)) {
                            method = AuthMethod.Email
                        }
                        MethodButton("Phone", Icons.Filled.Call, method == AuthMethod.Phone, Modifier.weight(1f)) {
                            method = AuthMethod.Phone
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (mode == AuthMode.Register) {
                        Field(Icons.Filled.Person, "Full name", fullName, { fullName = it })
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Field(
                        icon = if (method == AuthMethod.Email) Icons.Filled.Mail else Icons.Filled.Call,
                        placeholder = if (method == AuthMethod.Email) "you@example.com" else "+256 700 000 000",
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        keyboardType = if (method == AuthMethod.Email) KeyboardType.Email else KeyboardType.Phone,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Field(Icons.Filled.Lock, "Password", password, { password = it }, isPassword = true)

                    if (mode == AuthMode.Register) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectField(
                            icon = Icons.Filled.Language,
                            label = "Preferred language",
                            value = language,
                            options = listOf("English", "Luganda", "Runyankole", "Rukiga", "Lugbara", "Swahili"),
                            onSelect = { language = it },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SelectField(
                            icon = Icons.Filled.Park,
                            label = "National park",
                            value = park,
                            options = listOf(
                                "Bwindi Impenetrable", "Mgahinga Gorilla", "Murchison Falls",
                                "Queen Elizabeth", "Kibale", "Other / future parks",
                            ),
                            onSelect = { park = it },
                        )
                    }

                    if (mode == AuthMode.Login) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { notImplemented("Password reset") }) {
                                Text("Forgot password?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                    }

                    Button(
                        onClick = ::submit,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (mode == AuthMode.Login) "Sign in" else "Create account")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            "  or continue with  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Google/Apple: no social providers configured in Firebase yet
                        // (see PLAN.md §5) - rendered per design, surfaces "coming soon".
                        OutlinedButton(onClick = { notImplemented("Google sign-in") }, modifier = Modifier.weight(1f)) {
                            Text("Google")
                        }
                        OutlinedButton(onClick = { notImplemented("Apple sign-in") }, modifier = Modifier.weight(1f)) {
                            Text("Apple")
                        }
                    }

                    Text(
                        text = "By continuing you agree to our Terms and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MethodButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Field(
    icon: ImageVector,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SelectField(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    SilverBackSentryTheme {
        AuthScreen()
    }
}
