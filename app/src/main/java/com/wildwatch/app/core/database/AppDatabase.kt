package com.wildwatch.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        IncidentEntity::class,
        ClaimEntity::class,
        AlertEntity::class,
        ArticleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun claimDao(): ClaimDao
    abstract fun alertDao(): AlertDao
    abstract fun articleDao(): ArticleDao
}
