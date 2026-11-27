package com.shikkha.app.curriculum.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val id: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_bn") val nameBn: String,
    @SerialName("class_level") val classLevel: Int,
    val medium: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class ChapterDto(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_bn") val nameBn: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)
