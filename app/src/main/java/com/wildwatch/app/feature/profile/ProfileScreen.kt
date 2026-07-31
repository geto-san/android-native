package com.wildwatch.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.InstaBlue
import com.wildwatch.app.core.ui.theme.WildWatchTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onSignInClick: () -> Unit = {},
    onAccountManagementClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onSignOut = viewModel::signOut,
        onSignIn = onSignInClick,
        onThemeToggle = viewModel::setDarkTheme,
        onAccountManagementClick = onAccountManagementClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onAccountManagementClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                if (uiState.isGuest) {
                    GuestJoinCard(onSignIn = onSignIn)
                } else {
                    ProfileHeader(uiState)
                }
            }

            item {
                SettingsSection(
                    title = "ACCOUNT",
                    items = listOf(
                        SettingsItemData(
                            title = "Account management", 
                            icon = Icons.Default.AccountCircle, 
                            onClick = onAccountManagementClick
                        ),
                        SettingsItemData(
                            title = "Notifications", 
                            icon = Icons.Default.NotificationsNone, 
                            showSwitch = true
                        ),
                        SettingsItemData(
                            title = "Dark mode", 
                            icon = Icons.Default.DarkMode, 
                            showSwitch = true,
                            switchChecked = uiState.isDarkTheme,
                            onSwitchChange = onThemeToggle
                        ),
                        SettingsItemData(
                            title = "App language",
                            icon = Icons.Default.Language, 
                            trailingText = "English"
                        )
                    )
                )
            }

            item {
                SettingsSection(
                    title = "SUPPORT",
                    items = listOf(
                        if (uiState.isGuest) {
                            SettingsItemData(
                                title = "Sign in / Join WildWatch", 
                                icon = Icons.AutoMirrored.Filled.Logout, 
                                tint = InstaBlue, 
                                onClick = onSignIn
                            )
                        } else {
                            SettingsItemData(
                                title = "Sign out", 
                                icon = Icons.AutoMirrored.Filled.Logout, 
                                tint = MaterialTheme.colorScheme.error, 
                                onClick = onSignOut
                            )
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun GuestJoinCard(onSignIn: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Join the Community",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in to track your reports, earn badges, and help rangers protect Bwindi.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InstaBlue)
            ) {
                Text("Sign In", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ProfileHeader(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.displayName.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onTertiary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Bwindi Impenetrable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class SettingsItemData(
    val title: String,
    val icon: ImageVector,
    val trailingText: String? = null,
    val showSwitch: Boolean = false,
    val switchChecked: Boolean = false,
    val onSwitchChange: (Boolean) -> Unit = {},
    val tint: Color? = null,
    val onClick: () -> Unit = {}
)

@Composable
private fun SettingsSection(title: String, items: List<SettingsItemData>) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsRow(item)
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = item.tint ?: MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            color = item.tint ?: MaterialTheme.colorScheme.onSurface
        )

        if (item.showSwitch) {
            Switch(
                checked = item.switchChecked,
                onCheckedChange = item.onSwitchChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = InstaBlue
                )
            )
        } else if (item.trailingText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisteredProfilePreview() {
    WildWatchTheme {
        ProfileContent(
            uiState = ProfileUiState(
                displayName = "John Ranger",
                role = UserRole.RANGER,
                isGuest = false,
                primaryCount = 24,
                resolvedCount = 19
            ),
            onSignOut = {},
            onSignIn = {},
            onThemeToggle = {},
            onAccountManagementClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GuestProfilePreview() {
    WildWatchTheme {
        ProfileContent(
            uiState = ProfileUiState(
                isGuest = true
            ),
            onSignOut = {},
            onSignIn = {},
            onThemeToggle = {},
            onAccountManagementClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DarkRegisteredProfilePreview() {
    WildWatchTheme(darkTheme = true) {
        ProfileContent(
            uiState = ProfileUiState(
                displayName = "John Ranger",
                role = UserRole.RANGER,
                isGuest = false,
                primaryCount = 24,
                resolvedCount = 19,
                isDarkTheme = true
            ),
            onSignOut = {},
            onSignIn = {},
            onThemeToggle = {},
            onAccountManagementClick = {}
        )
    }
}
