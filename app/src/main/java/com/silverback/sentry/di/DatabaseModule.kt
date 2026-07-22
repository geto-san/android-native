package com.silverback.sentry.di

import android.content.Context
import androidx.room.Room
import com.silverback.sentry.data.local.db.AppDatabase
import com.silverback.sentry.data.local.db.ObservationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "silverback_sentry.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun providesObservationDao(database: AppDatabase): ObservationDao = database.observationDao()
}
