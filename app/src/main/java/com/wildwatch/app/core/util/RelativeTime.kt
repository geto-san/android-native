package com.wildwatch.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

fun relativeTime(createdAt: Long): String {
    val diffMs = System.currentTimeMillis() - createdAt
    return when {
        diffMs < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)} min"
        diffMs < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)} hr"
        diffMs < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> {
            val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}

fun relativeDay(reportedAt: String): String {
    val instant = runCatching { Instant.parse(reportedAt) }.getOrNull() ?: return reportedAt
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
    }
}
