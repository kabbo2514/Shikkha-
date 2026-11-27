package com.shikkha.app.mcq.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shikkha.app.auth.data.AuthRepository
import com.shikkha.app.core.datastore.SettingsStore
import com.shikkha.app.mcq.data.McqDto
import com.shikkha.app.mcq.data.McqRepository
import com.shikkha.app.mcq.data.PracticeAnswerDto
import com.shikkha.app.mcq.data.PracticeAttemptDto
import com.shikkha.app.mistakes.data.MISTAKES_MODE_ID
import com.shikkha.app.mistakes.data.MistakesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Content language follows the student's curriculum medium (Spec 18.3). */
enum class ContentLanguage { BANGLA, ENGLISH }

data class RecordedAnswer(
    val questionId: String,
    val selectedIndex: Int?,
    val isCorrect: Boolean,
    val timeSpentSeconds: Int,
)

data class PracticeUiState(
    val loading: Boolean = true,
    val fromCache: Boolean = false,
    val error: String? = null,
    val contentLanguage: ContentLanguage = ContentLanguage.BANGLA,
    val questions: List<McqDto> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val submitted: Boolean = false,
    val correctCount: Int = 0,
    val answers: List<RecordedAnswer> = emptyList(),
    val finished: Boolean = false,
    val attemptId: String? = null,
    val synced: Boolean = true,
    /** True when practising previously missed questions instead of one chapter. */
    val mistakeMode: Boolean = false,
) {
    val current: McqDto?
        get() = questions.getOrNull(index)
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: McqRepository,
    private val mistakesRepository: MistakesRepository,
    private val settingsStore: SettingsStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val chapterId: String = checkNotNull(savedStateHandle["chapterId"])
    private val mistakeMode: Boolean = chapterId == MISTAKES_MODE_ID
    private var questionStartedAt: Long = System.currentTimeMillis()
    private var loaded = false

    var state by mutableStateOf(PracticeUiState())
        private set

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val medium = authRepository.currentProfile()?.medium ?: "bangla"
            state = state.copy(
                mistakeMode = mistakeMode,
                contentLanguage = if (medium == "english") {
                    ContentLanguage.ENGLISH
                } else {
                    ContentLanguage.BANGLA
                }
            )
            val shuffle = settingsStore.shuffleQuestions.first()
            runCatching {
                if (mistakeMode) {
                    mistakesRepository.getRetryQuestions() to false
                } else {
                    repository.loadQuestions(chapterId)
                }
            }
                .onSuccess { (questions, fromCache) ->
                    state = state.copy(
                        loading = false,
                        fromCache = fromCache,
                        questions = if (shuffle) questions.shuffled() else questions,
                    )
                }
                .onFailure {
                    state = state.copy(loading = false, error = it.message ?: "load failed")
                }
        }
    }

    fun retry() {
        state = state.copy(loading = true, error = null)
        loaded = false
        load()
    }

    fun select(index: Int) {
        if (!state.submitted) state = state.copy(selected = index)
    }

    fun submit() {
        val question = state.current ?: return
        val selected = state.selected ?: return
        if (state.submitted) return
        val isCorrect = selected == question.correctIndex
        val elapsed = ((System.currentTimeMillis() - questionStartedAt) / 1000).toInt()
        state = state.copy(
            submitted = true,
            correctCount = state.correctCount + if (isCorrect) 1 else 0,
            answers = state.answers + RecordedAnswer(
                questionId = question.id,
                selectedIndex = selected,
                isCorrect = isCorrect,
                timeSpentSeconds = elapsed,
            ),
        )
    }

    fun next() {
        if (state.index >= state.questions.lastIndex) {
            finish()
        } else {
            questionStartedAt = System.currentTimeMillis()
            state = state.copy(index = state.index + 1, selected = null, submitted = false)
        }
    }

    private fun finish() {
        if (state.finished) return
        val attemptId = UUID.randomUUID().toString()
        state = state.copy(finished = true, attemptId = attemptId)
        viewModelScope.launch {
            val attempt = PracticeAttemptDto(
                id = attemptId,
                chapterId = if (mistakeMode) null else chapterId,
                mode = if (mistakeMode) "mistakes" else "chapter",
                totalQuestions = state.questions.size,
                correctCount = state.correctCount,
            )
            val answers = state.answers.map {
                PracticeAnswerDto(
                    attemptId = attemptId,
                    questionId = it.questionId,
                    selectedIndex = it.selectedIndex,
                    isCorrect = it.isCorrect,
                    timeSpentSeconds = it.timeSpentSeconds,
                )
            }
            val wrongIds = state.answers.filter { !it.isCorrect }.map { it.questionId }
            // In retry mode a correct answer clears that mistake from the library.
            val resolvedIds = if (mistakeMode) {
                state.answers.filter { it.isCorrect }.map { it.questionId }
            } else {
                emptyList()
            }
            val synced = repository.persistAttempt(attempt, answers, wrongIds, resolvedIds)
            state = state.copy(synced = synced)
        }
    }
}
