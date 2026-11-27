package com.shikkha.app.mcq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shikkha.app.R
import com.shikkha.app.core.common.UiState
import com.shikkha.app.mcq.data.McqDto
import com.shikkha.app.mcq.data.McqRepository
import com.shikkha.app.mcq.data.PracticeAnswerDto
import com.shikkha.app.mcq.data.PracticeAttemptDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewData(
    val attempt: PracticeAttemptDto,
    val answers: List<PracticeAnswerDto>,
    val questions: List<McqDto>,
)

@HiltViewModel
class PracticeResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: McqRepository,
) : ViewModel() {

    private val attemptId: String = checkNotNull(savedStateHandle["attemptId"])

    private val _uiState = MutableStateFlow<UiState<ReviewData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ReviewData>> = _uiState

    fun load() {
        if (_uiState.value !is UiState.Loading) return
        viewModelScope.launch {
            _uiState.value = try {
                val (attempt, answers, questions) = repository.getAttemptReview(attemptId)
                UiState.Success(ReviewData(attempt, answers, questions))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }

    fun reload() {
        _uiState.value = UiState.Loading
        load()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeResultScreen(
    onBackToChapters: () -> Unit,
    viewModel: PracticeResultViewModel = hiltViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackToChapters) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is UiState.Error -> com.shikkha.app.curriculum.ui.MessageWithRetry(
                message = stringResource(R.string.error_generic),
                onRetry = { viewModel.reload() },
                modifier = Modifier.padding(padding),
            )

            is UiState.Success -> ResultContent(
                data = s.data,
                onBackToChapters = onBackToChapters,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ResultContent(
    data: ReviewData,
    onBackToChapters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = data.attempt.totalQuestions
    val correct = data.attempt.correctCount
    val accuracy = if (total > 0) "${(correct * 100) / total}%" else "—"
    val questionsById = data.questions.associateBy { it.id }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.result_score, correct, total),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.result_accuracy, accuracy),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onBackToChapters) {
                        Text(stringResource(R.string.back_to_chapters))
                    }
                }
            }
        }

        items(data.answers, key = { it.questionId }) { answer ->
            val question = questionsById[answer.questionId]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        question?.questionBn ?: question?.questionEn ?: answer.questionId,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    val yourText = answer.selectedIndex?.let { idx ->
                        question?.options?.getOrNull(idx)?.textBn
                            ?: question?.options?.getOrNull(idx)?.textEn
                    } ?: "—"
                    val correctText = question?.options?.getOrNull(question.correctIndex)?.textBn
                        ?: question?.options?.getOrNull(question.correctIndex)?.textEn
                        ?: "—"
                    ReviewRow(
                        label = stringResource(R.string.your_answer),
                        value = yourText,
                        highlight = if (answer.isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    ReviewRow(
                        label = stringResource(R.string.correct_answer),
                        value = correctText,
                        highlight = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, highlight: androidx.compose.ui.graphics.Color) {
    Row {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = highlight)
    }
}
