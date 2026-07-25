package com.wildwatch.app.ui.claims

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.data.local.db.ClaimStatus
import com.wildwatch.app.domain.model.Claim
import com.wildwatch.app.ui.components.BackHeader
import com.wildwatch.app.ui.components.IconBadge
import com.wildwatch.app.ui.components.StatusPill
import com.wildwatch.app.ui.theme.Destructive
import com.wildwatch.app.ui.theme.Info
import com.wildwatch.app.ui.theme.Success
import com.wildwatch.app.ui.theme.SunsetAmber

private data class CategoryOption(val category: ClaimCategory, val icon: ImageVector, val title: String, val description: String)

private val CATEGORY_OPTIONS = listOf(
    CategoryOption(ClaimCategory.CROP_DESTRUCTION, Icons.Filled.Grass, "Crop destruction", "Maize, bananas, cassava and other crops damaged."),
    CategoryOption(ClaimCategory.LIVESTOCK_PREDATION, Icons.Filled.Agriculture, "Livestock predation", "Cattle, goats, sheep or poultry killed or injured."),
    CategoryOption(ClaimCategory.PROPERTY_DAMAGE, Icons.Filled.Cabin, "Property damage", "Homes, granaries, fences and structures damaged."),
    CategoryOption(ClaimCategory.HUMAN_INJURY, Icons.Filled.HeartBroken, "Human injury", "A person was hurt by wildlife."),
    CategoryOption(ClaimCategory.HUMAN_DEATH, Icons.Filled.PersonOff, "Human death", "A person lost their life due to wildlife."),
    CategoryOption(ClaimCategory.OTHER_LOSS, Icons.Filled.HelpOutline, "Other loss", "Any other loss covered by UWA regulations."),
)

// wireframe 12 & 12b - "My claims" and "Filing as" are now real data from
// ClaimViewModel/ClaimRepository, not hardcoded sample rows.
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
                BackHeader(title = "Compensation Claim", subtitle = "Select the type of loss", onBack = onBack)
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Filing as ${uiState.filingAsName} · ${uiState.filingAsPark}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            buildAnnotatedClaimNotice(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Text(
                        "CHOOSE CATEGORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                    )
                }
            }
            items(CATEGORY_OPTIONS) { option ->
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    CategoryRow(option, onClick = { onCategoryClick(option.category) })
                }
            }
            item {
                Text(
                    "My claims",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
            if (uiState.claims.isEmpty()) {
                item {
                    Text(
                        "No claims filed yet - choose a category above to file one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else {
                items(uiState.claims) { claim ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        ClaimRow(claim)
                    }
                }
            }
            item {
                Text(
                    "Compensation amounts are determined by UWA under the official guidelines and are not shown here.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }
}

private fun buildAnnotatedClaimNotice() = buildAnnotatedString {
    append("Claims follow the official ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append("UWA Compensation Regulations")
    }
    append(". A ranger verifies your incident before UWA reviews the claim.")
}

@Composable
private fun CategoryRow(option: CategoryOption, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = option.icon, background = MaterialTheme.colorScheme.secondary, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(option.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClaimRow(claim: Claim) {
    val (icon, color) = when (claim.status) {
        ClaimStatus.UNDER_VERIFICATION -> Icons.Filled.HourglassEmpty to SunsetAmber
        ClaimStatus.VERIFIED -> Icons.Filled.Verified to Info
        ClaimStatus.APPROVED, ClaimStatus.PAID -> Icons.Filled.CheckCircle to Success
        ClaimStatus.REJECTED -> Icons.Filled.CheckCircle to Destructive
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, background = color.copy(alpha = 0.15f), tint = color)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(claimReference(claim.id), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(claim.category.displayName(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(text = claim.status.displayName(), contentColor = color)
        }
    }
}

private fun claimReference(id: String): String = "CLM-" + id.take(6).uppercase()

private fun ClaimCategory.displayName(): String = when (this) {
    ClaimCategory.CROP_DESTRUCTION -> "Crop destruction"
    ClaimCategory.LIVESTOCK_PREDATION -> "Livestock predation"
    ClaimCategory.PROPERTY_DAMAGE -> "Property damage"
    ClaimCategory.HUMAN_INJURY -> "Human injury"
    ClaimCategory.HUMAN_DEATH -> "Human death"
    ClaimCategory.OTHER_LOSS -> "Other loss"
}

private fun ClaimStatus.displayName(): String = when (this) {
    ClaimStatus.UNDER_VERIFICATION -> "Under Verification"
    ClaimStatus.VERIFIED -> "Verified"
    ClaimStatus.APPROVED -> "Approved"
    ClaimStatus.PAID -> "Paid"
    ClaimStatus.REJECTED -> "Rejected"
}
