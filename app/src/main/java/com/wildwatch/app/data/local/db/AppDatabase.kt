package com.wildwatch.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        IncidentEntity::class,
        ClaimEntity::class,
        AlertEntity::class,
        ArticleEntity::class,
        NotificationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun claimDao(): ClaimDao
    abstract fun alertDao(): AlertDao
    abstract fun articleDao(): ArticleDao
    abstract fun notificationDao(): NotificationDao
}
