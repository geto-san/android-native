package com.wildwatch.app.ui.reportincident

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wildwatch.app.ui.components.IconBadge
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.PillTonalButton
import com.wildwatch.app.ui.theme.Success
import com.wildwatch.app.ui.theme.SunsetAmber

// wireframe 10 - shown after a sighting or conflict report saves.
@Composable
fun ReportSubmittedScreen(
    onFileCompensationClaim: () -> Unit,
    onReturnHome: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.size(96.dp).background(Success.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(Success.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(36.dp))
                }
            }

            Text(
                "Report submitted",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "Thank you for helping protect wildlife. Rangers have been notified and will follow up shortly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    IconBadge(icon = Icons.Filled.Receipt, background = SunsetAmber.copy(alpha = 0.18f), tint = SunsetAmber)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "Did this incident cause loss or damage?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "You can file a compensation claim now or anytime later from the dashboard. This step is optional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PillButton(text = "File a compensation claim", onClick = onFileCompensationClaim)
            Spacer(modifier = Modifier.padding(top = 4.dp))
            PillTonalButton(text = "Skip & return home", leadingIcon = Icons.Filled.Home, onClick = onReturnHome)
        }
    }
}
