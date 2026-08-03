package com.wildwatch.app.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(value)

    @TypeConverter
    fun fromRoutePointList(value: List<RoutePoint>): String = Json.encodeToString(value)

    @TypeConverter
    fun toRoutePointList(value: String): List<RoutePoint> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(value)
}
