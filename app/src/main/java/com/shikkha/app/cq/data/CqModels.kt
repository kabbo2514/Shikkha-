package com.shikkha.app.cq.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One part of a creative question: ক / খ / গ / ঘ (Spec 6.1). */
@Serializable
data class SubQuestionDto(
    val label: String = "",
    @SerialName("text_en") val textEn: String? = null,
    @SerialName("text_bn") val textBn: String? = null,
    val marks: Double = 0.0,
    @SerialName("model_answer_en") val modelAnswerEn: String? = null,
    @SerialName("model_answer_bn") val modelAnswerBn: String? = null,
)

/** Published creative question (stem + sub-questions), stored as jsonb in Supabase. */
@Serializable
data class CreativeQuestionDto(
    val id: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("stem_en") val stemEn: String? = null,
    @SerialName("stem_bn") val stemBn: String? = null,
    val subquestions: List<SubQuestionDto> = emptyList(),
    @SerialName("total_marks") val totalMarks: Double = 10.0,
)
