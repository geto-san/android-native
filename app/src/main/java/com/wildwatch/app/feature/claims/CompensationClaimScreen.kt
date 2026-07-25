package com.wildwatch.app.feature.claims

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.database.ClaimCategory
import com.wildwatch.app.core.database.ClaimStatus
import com.wildwatch.app.core.model.Claim
import com.wildwatch.app.core.ui.component.BackHeader
import com.wildwatch.app.core.ui.theme.Grey200
import com.wildwatch.app.core.ui.theme.Grey500

private data class CategoryOption(val category: ClaimCategory, val title: String)

private val CATEGORY_OPTIONS = listOf(
    CategoryOption(ClaimCategory.CROP_DESTRUCTION, "Crop destruction"),
    CategoryOption(ClaimCategory.LIVESTOCK_PREDATION, "Livestock predation"),
    CategoryOption(ClaimCategory.PROPERTY_DAMAGE, "Property damage"),
    CategoryOption(ClaimCategory.HUMAN_INJURY, "Human injury"),
    CategoryOption(ClaimCategory.HUMAN_DEATH, "Human death"),
    CategoryOption(ClaimCategory.OTHER_LOSS, "Other loss"),
)

@Composable
fun CompensationClaimScreen(
    onBack: () -> Unit,
    onCategoryClick: (ClaimCategory) -> Unit,
    viewModel: ClaimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                BackHeader(title = "Compensation", subtitle = "Select type of loss", onBack = onBack)
            }

            item {
                SectionHeader("File New Claim")
            }

            items(CATEGORY_OPTIONS) { option ->
                CategoryItem(option, onClick = { onCategoryClick(option.category) })
                HorizontalDivider(thickness = 0.5.dp, color = Grey200, modifier = Modifier.padding(start = 16.dp))
            }

            item {
                SectionHeader("My Active Claims")
            }

            if (uiState.claims.isEmpty()) {
                item {
                    EmptyClaimsState()
                }
            } else {
                items(uiState.claims) { claim ->
                    ClaimItem(claim)
                    HorizontalDivider(thickness = 0.5.dp, color = Grey200, modifier = Modifier.padding(start = 16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Claims are reviewed by official UWA regulations.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Grey500,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun CategoryItem(option: CategoryOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Grey200))
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Grey500, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ClaimItem(claim: Claim) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Claim #${claim.id.take(6).uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = claim.category.name.lowercase().replace("_", " "),
                style = MaterialTheme.typography.bodySmall,
                color = Grey500
            )
        }
        Text(
            text = claim.status.name.lowercase().replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyClaimsState() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("No claims found.", style = MaterialTheme.typography.bodySmall, color = Grey500)
    }
}
