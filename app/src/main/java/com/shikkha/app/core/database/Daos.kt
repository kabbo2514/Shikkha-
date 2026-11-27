package com.shikkha.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedQuestionDao {

    @Query("SELECT payloadJson FROM cached_questions WHERE chapterId = :chapterId")
    suspend fun payloadsForChapter(chapterId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE chapterId = :chapterId")
    suspend fun clearChapter(chapterId: String)
}

@Dao
interface QueuedAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: QueuedAttemptEntity)

    @Query("SELECT * FROM queued_attempts WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun pending(): List<QueuedAttemptEntity>

    @Query("UPDATE queued_attempts SET synced = 1 WHERE attemptId = :attemptId")
    suspend fun markSynced(attemptId: String)

    @Query("DELETE FROM queued_attempts WHERE synced = 1")
    suspend fun pruneSynced()
}
