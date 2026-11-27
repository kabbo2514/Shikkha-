package com.shikkha.app.mistakes.data

import com.shikkha.app.mcq.data.McqDto
import com.shikkha.app.mcq.data.MistakeRowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/** Route argument that switches the practice screen into mistake-retry mode. */
const val MISTAKES_MODE_ID = "mistakes"

/** Keeps one retry session short enough to finish (Spec 8.2). */
const val MISTAKE_PRACTICE_LIMIT = 20

data class MistakeItem(
    val question: McqDto,
    val repetitionCount: Int,
)

/**
 * Reads the student's own unresolved mistakes. Row Level Security limits `mistakes`
 * to the signed-in student and `mcqs` to published questions (Spec 8.4).
 */
@Singleton
class MistakesRepository @Inject constructor(
    private val client: SupabaseClient,
) {

    /** Most recently missed first. Questions that are no longer published are skipped. */
    suspend fun getUnresolved(): List<MistakeItem> {
        val rows = client.from("mistakes")
            .select {
                filter { eq("resolved", false) }
                order("last_seen_at", Order.DESCENDING)
            }
            .decodeList<MistakeRowDto>()
        if (rows.isEmpty()) return emptyList()

        val questions = client.from("mcqs")
            .select { filter { isIn("id", rows.map { it.questionId }) } }
            .decodeList<McqDto>()
            .associateBy { it.id }

        return rows.mapNotNull { row ->
            questions[row.questionId]?.let { MistakeItem(it, row.repetitionCount) }
        }
    }

    suspend fun getRetryQuestions(): List<McqDto> =
        getUnresolved().map { it.question }.take(MISTAKE_PRACTICE_LIMIT)
}
