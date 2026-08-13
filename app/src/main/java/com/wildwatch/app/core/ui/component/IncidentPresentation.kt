package com.wildwatch.app.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.wildwatch.app.core.database.IncidentSeverity
import com.wildwatch.app.core.database.IncidentStatus
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Success
import com.wildwatch.app.core.ui.theme.SunsetAmber

// Single source of truth for how severity/status/type map to color, label and icon across
// the app (Response Center cards, incident detail) - was previously duplicated per-screen.
@Composable
fun severityColor(severity: IncidentSeverity): Color = when (severity) {
    IncidentSeverity.MEDIUM -> SunsetAmber
    IncidentSeverity.HIGH -> Destructive
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun statusColor(status: IncidentStatus): Color = when (status) {
    IncidentStatus.OPEN -> Destructive
    IncidentStatus.IN_PROGRESS -> SunsetAmber
    IncidentStatus.RESOLVED -> Success
}

fun statusLabel(status: IncidentStatus): String = when (status) {
    IncidentStatus.OPEN -> "Needs response"
    IncidentStatus.IN_PROGRESS -> "In progress"
    IncidentStatus.RESOLVED -> "Resolved"
}

fun typeIcon(type: IncidentType): ImageVector = when (type) {
    IncidentType.SIGHTING -> Icons.Filled.Visibility
    IncidentType.CONFLICT -> Icons.Filled.Groups
    IncidentType.EMERGENCY -> Icons.Filled.ReportProblem
    IncidentType.POACHING -> Icons.Filled.Dangerous
    IncidentType.SNARE -> Icons.Filled.Warning
}

fun typeLabel(type: IncidentType): String = when (type) {
    IncidentType.SIGHTING -> "Sighting"
    IncidentType.CONFLICT -> "Conflict"
    IncidentType.EMERGENCY -> "Emergency"
    IncidentType.POACHING -> "Poaching"
    IncidentType.SNARE -> "Snare"
}
