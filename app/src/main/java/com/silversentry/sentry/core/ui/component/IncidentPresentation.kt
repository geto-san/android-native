package com.silversentry.sentry.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.silversentry.sentry.core.database.IncidentSeverity
import com.silversentry.sentry.core.database.IncidentStatus
import com.silversentry.sentry.core.database.IncidentType
import com.silversentry.sentry.core.model.Incident
import com.silversentry.sentry.core.ui.theme.Destructive
import com.silversentry.sentry.core.ui.theme.Success
import com.silversentry.sentry.core.ui.theme.SunsetAmber

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
    // A withdrawn alert is no longer anyone's problem to act on - muted to read as
    // inactive rather than urgent.
    IncidentStatus.CANCELLED -> MaterialTheme.colorScheme.outline
}

fun statusLabel(status: IncidentStatus): String = when (status) {
    IncidentStatus.OPEN -> "Needs response"
    IncidentStatus.IN_PROGRESS -> "In progress"
    IncidentStatus.RESOLVED -> "Resolved"
    IncidentStatus.CANCELLED -> "Cancelled"
}

fun typeIcon(type: IncidentType): ImageVector = when (type) {
    IncidentType.SIGHTING -> Icons.Filled.Visibility
    IncidentType.CONFLICT -> Icons.Filled.Groups
    IncidentType.EMERGENCY -> Icons.Filled.ReportProblem
    IncidentType.POACHING -> Icons.Filled.Dangerous
    IncidentType.SNARE -> Icons.Filled.Warning
    IncidentType.SOS -> Icons.Filled.Sos
}

fun typeLabel(type: IncidentType): String = when (type) {
    IncidentType.SIGHTING -> "Sighting"
    IncidentType.CONFLICT -> "Conflict"
    IncidentType.EMERGENCY -> "Emergency"
    IncidentType.POACHING -> "Poaching"
    IncidentType.SNARE -> "Snare"
    IncidentType.SOS -> "SOS"
}

// The title shown for an incident card/header. Sightings are titled by species; every
// other type (SOS, conflict, poaching, ...) has no species, so the reporter's own
// description - the detailed info - becomes the title, falling back to the type label
// when it's empty. This replaces the old stored "N/A" sentinel (ReportIncidentViewModel
// wrote it for non-sighting types) being shown verbatim as the title.
fun Incident.displayTitle(): String = when (type) {
    IncidentType.SIGHTING -> species
    else -> summary?.trim()?.takeIf { it.isNotEmpty() } ?: typeLabel(type)
}
