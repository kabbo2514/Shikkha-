package com.shikkha.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline cache of published MCQs (Spec 21.1). The question payload is stored
 * as serialized JSON so the cache survives app updates to the DTO fields.
 */
@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val payloadJson: String,
    val fetchedAt: Long,
)

/**
 * Queue of practice attempts that could not be synced yet (Spec 21.3).
 * The client-generated attempt id is the idempotency key: re-sending the
 * same id never creates a duplicate row because of the primary key.
 */
@Entity(tableName = "queued_attempts")
data class QueuedAttemptEntity(
    @PrimaryKey val attemptId: String,
    val chapterId: String,
    val payloadJson: String,
    val createdAt: Long,
    val synced: Boolean = false,
)
