package com.shikkha.app.cq.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CqRepository @Inject constructor(
    private val client: SupabaseClient,
) {

    /** Published creative questions of one chapter (unpublished ones are hidden by RLS). */
    suspend fun getForChapter(chapterId: String): List<CreativeQuestionDto> =
        client.from("creative_questions")
            .select {
                filter {
                    eq("chapter_id", chapterId)
                    eq("status", "published")
                }
            }
            .decodeList<CreativeQuestionDto>()
}
