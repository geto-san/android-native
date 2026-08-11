package com.wildwatch.app.core.di

import android.content.Context
import androidx.room.Room
import com.wildwatch.app.core.database.AlertDao
import com.wildwatch.app.core.database.AppDatabase
import com.wildwatch.app.core.database.ArticleDao
import com.wildwatch.app.core.database.IncidentDao
import com.wildwatch.app.core.database.NotificationDao
import com.wildwatch.app.core.database.PatrolLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

private const val DATABASE_NAME = "wildwatch.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Integrate SQLCipher for on-device encryption.
        // TODO: In production, derive this passphrase from a key stored securely
        // in the Android Keystore rather than using a hardcoded string.
        val passphrase = SQLiteDatabase.getBytes("wildwatch-secure-storage-key".toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(factory)
            // Pre-launch app, no installed base to preserve - a future schema
            // change falls back to a fresh database rather than requiring a
            // real Migration written against data nobody has yet.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providesIncidentDao(database: AppDatabase): IncidentDao = database.incidentDao()

    @Provides
    fun providesAlertDao(database: AppDatabase): AlertDao = database.alertDao()

    @Provides
    fun providesArticleDao(database: AppDatabase): ArticleDao = database.articleDao()

    @Provides
    fun providesNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun providesPatrolLogDao(database: AppDatabase): PatrolLogDao = database.patrolLogDao()
}
