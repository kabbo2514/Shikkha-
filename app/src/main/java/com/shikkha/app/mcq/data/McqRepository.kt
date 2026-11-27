package com.shikkha.app.mcq.data

import com.shikkha.app.core.database.CachedQuestionDao
import com.shikkha.app.core.database.CachedQuestionEntity
import com.shikkha.app.core.database.QueuedAttemptDao
import com.shikkha.app.core.database.QueuedAttemptEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class McqRepository @Inject constructor(
    private val client: SupabaseClient,
    private val cachedQuestionDao: CachedQuestionDao,
    private val queuedAttemptDao: QueuedAttemptDao,
    private val json: Json,
) {

    /**
     * Online-first with offline fallback (Spec 21.1). Returns the questions and
     * whether they came from the local cache.
     */
    suspend fun loadQuestions(chapterId: String): Pair<List<McqDto>, Boolean> = try {
        val questions = client.from("mcqs")
            .select {
                filter {
                    eq("chapter_id", chapterId)
                    eq("status", "published")
                }
            }
            .decodeList<McqDto>()
        cachedQuestionDao.clearChapter(chapterId)
        cachedQuestionDao.upsertAll(
            questions.map {
                CachedQuestionEntity(
                    id = it.id,
                    chapterId = chapterId,
                    payloadJson = json.encodeToString(McqDto.serializer(), it),
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        )
        questions to false
    } catch (e: Exception) {
        val cached = cachedQuestionDao.payloadsForChapter(chapterId)
            .map { json.decodeFromString(McqDto.serializer(), it) }
        if (cached.isEmpty()) throw e
        cached to true
    }

    /**
     * Persists the completed attempt. On failure the whole bundle is queued
     * locally and can be retried via [syncQueuedAttempts] after reconnecting.
     */
    suspend fun persistAttempt(
        attempt: PracticeAttemptDto,
        answers: List<PracticeAnswerDto>,
        wrongQuestionIds: List<String>,
        resolvedQuestionIds: List<String> = emptyList(),
    ): Boolean = try {
        client.from("practice_attempts").insert(attempt)
        if (answers.isNotEmpty()) client.from("practice_answers").insert(answers)
        recordMistakes(wrongQuestionIds)
        resolveMistakes(resolvedQuestionIds)
        true
    } catch (e: Exception) {
        queuedAttemptDao.enqueue(
            QueuedAttemptEntity(
                attemptId = attempt.id,
                chapterId = attempt.chapterId ?: "",
                payloadJson = json.encodeToString(
                    SyncBundle.serializer(),
                    SyncBundle(attempt, answers, wrongQuestionIds, resolvedQuestionIds),
                ),
                createdAt = System.currentTimeMillis(),
            )
        )
        false
    }

    suspend fun getAttemptReview(
        attemptId: String,
    ): Triple<PracticeAttemptDto, List<PracticeAnswerDto>, List<McqDto>> {
        val attempt = client.from("practice_attempts")
            .select { filter { eq("id", attemptId) } }
            .decodeSingle<PracticeAttemptDto>()
        val answers = client.from("practice_answers")
            .select { filter { eq("attempt_id", attemptId) } }
            .decodeList<PracticeAnswerDto>()
        val questionIds = answers.map { it.questionId }
        val questions = if (questionIds.isEmpty()) {
            emptyList()
        } else {
            client.from("mcqs")
                .select { filter { isIn("id", questionIds) } }
                .decodeList<McqDto>()
        }
        return Triple(attempt, answers, questions)
    }

    /** Retries locally queued attempts (called from WorkManager in later phases). */
    suspend fun syncQueuedAttempts() {
        queuedAttemptDao.pending().forEach { entity ->
            runCatching {
                val bundle = json.decodeFromString(SyncBundle.serializer(), entity.payloadJson)
                client.from("practice_attempts").insert(bundle.attempt)
                if (bundle.answers.isNotEmpty()) client.from("practice_answers").insert(bundle.answers)
                recordMistakes(bundle.wrongQuestionIds)
                resolveMistakes(bundle.resolvedQuestionIds)
                queuedAttemptDao.markSynced(entity.attemptId)
            }
        }
        queuedAttemptDao.pruneSynced()
    }

    /**
     * New wrong answers become mistake rows. A question that is already known is
     * counted again and re-opened if it had been resolved (Spec 8.1 / 8.4).
     */
    private suspend fun recordMistakes(wrongIds: List<String>) {
        if (wrongIds.isEmpty()) return
        val uid = client.auth.currentUserOrNull()?.id ?: return
        val existing = client.from("mistakes")
            .select {
                filter {
                    eq("student_id", uid)
                    isIn("question_id", wrongIds)
                }
            }
            .decodeList<MistakeRowDto>()
        val known = existing.associateBy { it.questionId }

        val missing = wrongIds.filter { it !in known }.map { MistakeInsert(it) }
        if (missing.isNotEmpty()) client.from("mistakes").insert(missing)

        val now = Instant.now().toString()
        known.values.forEach { row ->
            client.from("mistakes").update({
                set("repetition_count", row.repetitionCount + 1)
                set("resolved", false)
                set("last_seen_at", now)
            }) {
                filter {
                    eq("student_id", uid)
                    eq("question_id", row.questionId)
                }
            }
        }
    }

    /** Questions answered correctly in mistake-retry mode are marked resolved. */
    private suspend fun resolveMistakes(correctIds: List<String>) {
        if (correctIds.isEmpty()) return
        val uid = client.auth.currentUserOrNull()?.id ?: return
        client.from("mistakes").update({
            set("resolved", true)
            set("last_seen_at", Instant.now().toString())
        }) {
            filter {
                eq("student_id", uid)
                isIn("question_id", correctIds)
            }
        }
    }
}
