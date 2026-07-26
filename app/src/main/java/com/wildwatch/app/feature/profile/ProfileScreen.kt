package com.wildwatch.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.core.ui.component.StatusPill
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        uiState.displayName.lowercase().replace(" ", "_"), 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
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
                .padding(paddingValues)
        ) {
            item {
                ProfileHeader(uiState)
            }
            
            item {
                ProfileBio(uiState)
            }

            item {
                ProfileActions(onSignOut = viewModel::signOut)
            }

            item {
                ProfileFieldSection(uiState)
            }

            item {
                HorizontalDivider(thickness = 0.5.dp, color = Grey200, modifier = Modifier.padding(top = 16.dp))
                // Grid of sightings could go here
            }
        }
    }
}

@Composable
private fun ProfileHeader(uiState: ProfileUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Grey200)
        )
        
        Row(
            modifier = Modifier.weight(1f).padding(start = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = if (uiState.role == UserRole.RANGER) "Assigned" else "Reports",
                value = uiState.primaryCount.toString(),
            )
            StatItem(label = "Resolved", value = uiState.resolvedCount.toString())
            StatItem(label = "Zones", value = uiState.zones.size.toString())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ProfileBio(uiState: ProfileUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = uiState.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
                text = if (uiState.role == UserRole.RANGER) "Field Ranger" else "Community member",
                contentColor = if (uiState.role == UserRole.RANGER) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        Text(
            text = "Conservation advocate in Bwindi Impenetrable.\nProtecting wildlife, empowering communities.",
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp
        )
        uiState.email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileFieldSection(uiState: ProfileUiState) {
    // Ranger-only for now: wireframe's community "My badges" row has no
    // backing gamification data source yet, so it's left out rather than
    // faked with a static number.
    if (uiState.role != UserRole.RANGER) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Assigned zones",
            style = MaterialTheme.typography.labelSmall,
            color = Grey500,
        )
        Text(
            text = if (uiState.zones.isEmpty()) "No active assignments" else uiState.zones.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProfileActions(onSignOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {},
            modifier = Modifier.weight(1f).height(32.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(containerColor = Grey200, contentColor = Color.Black),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Edit profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = {},
            modifier = Modifier.weight(1f).height(32.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(containerColor = Grey200, contentColor = Color.Black),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Share profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onSignOut,
            modifier = Modifier.height(32.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("Log Out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
