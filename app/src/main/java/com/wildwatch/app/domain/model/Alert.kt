package com.wildwatch.app.domain.model

import com.wildwatch.app.data.local.db.AlertCategory
import com.wildwatch.app.data.local.db.AlertEntity
import com.wildwatch.app.data.local.db.AlertSeverity

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
