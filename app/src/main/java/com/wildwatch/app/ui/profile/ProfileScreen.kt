package com.wildwatch.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Settings rows below the sign-out button are static/decorative for now - no
// backend concept exists yet for notifications, language, assigned zones,
// shift preferences, or a coordination radio channel, so building real
// persistence for them would be speculative.
private val DECORATIVE_SETTINGS = listOf(
    "Notifications",
    "Password",
    "Language",
    "Assigned zones",
    "Shift preferences",
    "Coordination radio channel",
    "Privacy & Help",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                IdentityCard(uiState.displayName, uiState.email, uiState.resolvedCount)

                Column(modifier = Modifier.padding(top = 24.dp)) {
                    DECORATIVE_SETTINGS.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        )
                    }
                }

                Button(onClick = viewModel::signOut, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(displayName: String, email: String?, resolvedCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(text = displayName, style = MaterialTheme.typography.titleLarge)
                    email?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            SuggestionChip(onClick = {}, label = { Text("On duty") }, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Resolved: $resolvedCount",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
