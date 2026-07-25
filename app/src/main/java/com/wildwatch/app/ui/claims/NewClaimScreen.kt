package com.wildwatch.app.ui.claims

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.WildWatchTextField

// The claim-detail step the wireframes' category chevron implies but never
// draws in full - kept intentionally minimal (description only, no photo
// evidence) since that's the one input the domain actually requires.
@Composable
fun NewClaimScreen(
    category: ClaimCategory,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: NewClaimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedClaimId) {
        if (uiState.savedClaimId != null) {
            viewModel.consumeSavedEvent()
            onSubmitted()
        }
    }

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackHeader(title = category.displayName(), subtitle = "Tell us what happened", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "DESCRIPTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                WildWatchTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    placeholder = "What was lost or damaged, and when?",
                    singleLine = false,
                    minLines = 5,
                )

                uiState.saveError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Box(modifier = Modifier.padding(top = 20.dp)) {
                    PillButton(
                        text = "Submit claim",
                        loading = uiState.isSaving,
                        onClick = { viewModel.submit(category) },
                    )
                }
            }
        }
    }
}

private fun ClaimCategory.displayName(): String = when (this) {
    ClaimCategory.CROP_DESTRUCTION -> "Crop destruction"
    ClaimCategory.LIVESTOCK_PREDATION -> "Livestock predation"
    ClaimCategory.PROPERTY_DAMAGE -> "Property damage"
    ClaimCategory.HUMAN_INJURY -> "Human injury"
    ClaimCategory.HUMAN_DEATH -> "Human death"
    ClaimCategory.OTHER_LOSS -> "Other loss"
}
