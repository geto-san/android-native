package com.wildwatch.app.core.data.incident

import com.wildwatch.app.core.model.Incident

data class RemoteIncidentChange(
    val incident: Incident,
    val isRemoved: Boolean = false,
)
