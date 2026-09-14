package com.silversentry.sentry.core.data.incident

import com.silversentry.sentry.core.model.Incident

data class RemoteIncidentChange(
    val incident: Incident,
    val isRemoved: Boolean = false,
)
