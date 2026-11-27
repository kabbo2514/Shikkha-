package com.shikkha.app.mcq.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class McqOptionDto(
    @SerialName("text_en") val textEn: String? = null,
    @SerialName("text_bn") val textBn: String? = null,
)

/** Published MCQ as stored in Supabase (Spec 4.3). */
@Serializable
data class McqDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("question_en") val questionEn: String? = null,
    @SerialName("question_bn") val questionBn: String? = null,
    val options: List<McqOptionDto> = emptyList(),
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("explanation_en") val explanationEn: String? = null,
    @SerialName("explanation_bn") val explanationBn: String? = null,
    val difficulty: Int = 1,
    @SerialName("image_url") val imageUrl: String? = null,
)

/** Idempotent attempt record; [id] is generated on the client as the idempotency key. */
@Serializable
data class PracticeAttemptDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String? = null,
    val mode: String = "chapter",
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("correct_count") val correctCount: Int,
)

@Serializable
data class PracticeAnswerDto(
    @SerialName("attempt_id") val attemptId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("selected_index") val selectedIndex: Int? = null,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("time_spent_seconds") val timeSpentSeconds: Int,
)

@Serializable
data class MistakeRowDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("repetition_count") val repetitionCount: Int = 1,
    val resolved: Boolean = false,
)

@Serializable
data class MistakeInsert(
    @SerialName("question_id") val questionId: String,
)

/** Offline queue payload (Spec 21.3). */
@Serializable
data class SyncBundle(
    val attempt: PracticeAttemptDto,
    val answers: List<PracticeAnswerDto>,
    val wrongQuestionIds: List<String>,
    // Added with mistake-retry mode; default keeps older queued payloads readable.
    val resolvedQuestionIds: List<String> = emptyList(),
)
