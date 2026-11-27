package com.shikkha.app.mcq.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shikkha.app.R
import com.shikkha.app.curriculum.ui.MessageWithRetry
import com.shikkha.app.mcq.data.McqDto
import com.shikkha.app.theme.CorrectGreen
import com.shikkha.app.theme.IncorrectRed

/**
 * Single-question practice UI with immediate feedback (Spec 5.2 / 5.3).
 * This is PRACTICE mode: answers and explanations are shown after submission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onFinish: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state

    // Navigation happens once persistence has at least been queued.
    LaunchedEffect(state.finished, state.attemptId) {
        val id = state.attemptId
        if (state.finished && id != null) onFinish(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.mistakeMode) R.string.mistakes_retry_title else R.string.practice_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> MessageWithRetry(
                message = stringResource(R.string.practice_error_loading),
                onRetry = { viewModel.retry() },
                modifier = Modifier.padding(padding),
            )

            state.questions.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(
                        if (state.mistakeMode) R.string.mistakes_empty else R.string.empty_questions
                    )
                )
            }

            else -> PracticeContent(
                state = state,
                onSelect = viewModel::select,
                onSubmit = viewModel::submit,
                onNext = viewModel::next,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun PracticeContent(
    state: PracticeUiState,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = state.current ?: return
    val progress = (state.index + 1).toFloat() / state.questions.size

    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.question_progress, state.index + 1, state.questions.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.fromCache) {
            Text(
                text = stringResource(R.string.offline_cached_notice),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = questionText(question, state.contentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            question.options.forEachIndexed { optionIndex, option ->
                val optionState = when {
                    state.submitted && optionIndex == question.correctIndex -> OptionVisual.CORRECT
                    state.submitted && optionIndex == state.selected -> OptionVisual.WRONG_SELECTED
                    optionIndex == state.selected -> OptionVisual.SELECTED
                    else -> OptionVisual.DEFAULT
                }
                OptionRow(
                    text = optionText(option, state.contentLanguage),
                    visual = optionState,
                    enabled = !state.submitted,
                    onClick = { onSelect(optionIndex) },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.submitted) {
                val correct = state.selected == question.correctIndex
                Text(
                    text = stringResource(
                        if (correct) R.string.correct_feedback else R.string.incorrect_feedback
                    ),
                    color = if (correct) CorrectGreen else IncorrectRed,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics {
                        contentDescription = if (correct) "Correct" else "Incorrect"
                    },
                )
                Spacer(Modifier.height(8.dp))
                val explanation = if (state.contentLanguage == ContentLanguage.BANGLA) {
                    question.explanationBn ?: question.explanationEn
                } else {
                    question.explanationEn ?: question.explanationBn
                }
                if (!explanation.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                stringResource(R.string.explanation_title),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.submitted) {
                Button(
                    onClick = onSubmit,
                    enabled = state.selected != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.submit_answer)) }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.index >= state.questions.lastIndex) {
                            stringResource(R.string.finish_practice)
                        } else {
                            stringResource(R.string.next_question)
                        }
                    )
                }
            }
        }
    }
}

private enum class OptionVisual { DEFAULT, SELECTED, CORRECT, WRONG_SELECTED }

@Composable
private fun OptionRow(
    text: String,
    visual: OptionVisual,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when (visual) {
        OptionVisual.CORRECT -> CorrectGreen
        OptionVisual.WRONG_SELECTED -> IncorrectRed
        OptionVisual.SELECTED -> MaterialTheme.colorScheme.primary
        OptionVisual.DEFAULT -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when (visual) {
        OptionVisual.CORRECT -> CorrectGreen.copy(alpha = 0.12f)
        OptionVisual.WRONG_SELECTED -> IncorrectRed.copy(alpha = 0.12f)
        OptionVisual.SELECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        OptionVisual.DEFAULT -> Color.Transparent
    }
    Surface(
        border = BorderStroke(1.dp, borderColor),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun questionText(question: McqDto, language: ContentLanguage): String =
    if (language == ContentLanguage.BANGLA) {
        question.questionBn ?: question.questionEn.orEmpty()
    } else {
        question.questionEn ?: question.questionBn.orEmpty()
    }

private fun optionText(option: com.shikkha.app.mcq.data.McqOptionDto, language: ContentLanguage): String =
    if (language == ContentLanguage.BANGLA) {
        option.textBn ?: option.textEn.orEmpty()
    } else {
        option.textEn ?: option.textBn.orEmpty()
    }
