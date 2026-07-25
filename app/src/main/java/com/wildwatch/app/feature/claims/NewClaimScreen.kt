package com.wildwatch.app.feature.claims

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.ClaimCategory
import com.wildwatch.app.core.database.Park
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.component.WildWatchDropdownField
import com.wildwatch.app.core.ui.component.WildWatchTextField
import com.wildwatch.app.core.ui.theme.Grey500

@Composable
fun NewClaimScreen(
    category: ClaimCategory,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: NewClaimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPark by remember { mutableStateOf(Park.BWINDI_IMPENETRABLE) }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) onSubmitted()
    }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = "File Claim", subtitle = category.name.lowercase().replace('_', ' '), onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                FieldLabel("National Park")
                WildWatchDropdownField(
                    value = selectedPark,
                    options = Park.entries,
                    onSelect = { selectedPark = it },
                    displayName = { it.name.replace('_', ' ') }
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                FieldLabel("Description of Loss")
                WildWatchTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Detailed account of the incident…",
                    singleLine = false,
                    minLines = 6,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error, 
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall, 
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { viewModel.submitClaim(category, selectedPark, description) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    enabled = description.isNotBlank() && !uiState.isSaving
                ) {
                    Text(if (uiState.isSaving) "Submitting..." else "Submit Claim", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Grey500,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
