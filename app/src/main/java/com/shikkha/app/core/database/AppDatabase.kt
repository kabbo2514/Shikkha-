package com.shikkha.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [CachedQuestionEntity::class, QueuedAttemptEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedQuestionDao(): CachedQuestionDao
    abstract fun queuedAttemptDao(): QueuedAttemptDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "shikkha.db").build()

    @Provides
    fun provideCachedQuestionDao(db: AppDatabase): CachedQuestionDao = db.cachedQuestionDao()

    @Provides
    fun provideQueuedAttemptDao(db: AppDatabase): QueuedAttemptDao = db.queuedAttemptDao()
}
