package com.shikkha.app.curriculum.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurriculumRepository @Inject constructor(
    private val client: SupabaseClient,
) {

    /** Subject availability depends on class + medium (Spec 4.2). */
    suspend fun getSubjects(classLevel: Int, medium: String): List<SubjectDto> =
        client.from("subjects")
            .select {
                filter {
                    eq("class_level", classLevel)
                    eq("medium", medium)
                }
                order("sort_order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList()

    suspend fun getChapters(subjectId: String): List<ChapterDto> =
        client.from("chapters")
            .select {
                filter { eq("subject_id", subjectId) }
                order("sort_order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList()

    suspend fun countPublishedQuestions(chapterId: String): Long =
        client.from("mcqs")
            .select {
                filter {
                    eq("chapter_id", chapterId)
                    eq("status", "published")
                }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }
            .countOrNull() ?: 0L
}
