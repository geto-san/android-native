package com.wildwatch.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        IncidentEntity::class,
        AlertEntity::class,
        ArticleEntity::class,
        NotificationEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun alertDao(): AlertDao
    abstract fun articleDao(): ArticleDao
    abstract fun notificationDao(): NotificationDao
}
