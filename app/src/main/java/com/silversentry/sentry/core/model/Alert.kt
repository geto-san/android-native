package com.silversentry.sentry.core.model

import com.silversentry.sentry.core.database.AlertCategory
import com.silversentry.sentry.core.database.AlertEntity
import com.silversentry.sentry.core.database.AlertSeverity

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val category: AlertCategory,
    val severity: AlertSeverity,
    val createdAt: Long,
) {
    fun toEntity(): AlertEntity = AlertEntity(
        id = id,
        title = title,
        description = description,
        location = location,
        category = category,
        severity = severity,
        createdAt = createdAt,
    )

    companion object {
        fun fromEntity(entity: AlertEntity): Alert = Alert(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            location = entity.location,
            category = entity.category,
            severity = entity.severity,
            createdAt = entity.createdAt,
        )
    }
}
