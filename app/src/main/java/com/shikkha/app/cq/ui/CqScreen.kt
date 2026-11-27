package com.shikkha.app.cq.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shikkha.app.R
import com.shikkha.app.auth.data.AuthRepository
import com.shikkha.app.core.common.UiState
import com.shikkha.app.cq.data.CqRepository
import com.shikkha.app.cq.data.CreativeQuestionDto
import com.shikkha.app.cq.data.SubQuestionDto
import com.shikkha.app.curriculum.ui.MessageWithRetry
import com.shikkha.app.mcq.ui.ContentLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CqContent(
    val questions: List<CreativeQuestionDto>,
    val language: ContentLanguage,
)

@HiltViewModel
class CqViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CqRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val chapterId: String = checkNotNull(savedStateHandle["chapterId"])

    private val _uiState = MutableStateFlow<UiState<CqContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<CqContent>> = _uiState

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val medium = authRepository.currentProfile()?.medium ?: "bangla"
                val language = if (medium == "english") ContentLanguage.ENGLISH else ContentLanguage.BANGLA
                UiState.Success(CqContent(repository.getForChapter(chapterId), language))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }
}

/**
 * Creative-question browser with model answers (Spec 6.1-6.2). Read-only study view:
 * tap a question to open its parts, then reveal each model answer when ready.
 * Writing practice and drafts (Spec 6.3-6.4) are still PLANNED.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CqScreen(
    onBack: () -> Unit,
    viewModel: CqViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

            is UiState.Error -> MessageWithRetry(
                message = stringResource(R.string.error_generic),
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding),
            )

            is UiState.Success -> {
                if (s.data.questions.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.cq_empty),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                        items(s.data.questions, key = { it.id }) { cq ->
                            CqCard(cq, s.data.language)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CqCard(cq: CreativeQuestionDto, language: ContentLanguage) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(cq.stemText(language), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.cq_total_marks, formatMarks(cq.totalMarks)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (expanded) {
                cq.subquestions.forEach { sub ->
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    SubQuestionRow(sub, language)
                }
            }
        }
    }
}

@Composable
private fun SubQuestionRow(sub: SubQuestionDto, language: ContentLanguage) {
    var showAnswer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                sub.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )
            Text(
                sub.questionText(language),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "[${formatMarks(sub.marks)}]",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { showAnswer = !showAnswer }) {
                Text(
                    stringResource(
                        if (showAnswer) R.string.cq_hide_answer else R.string.cq_show_answer
                    )
                )
            }
        }
        if (showAnswer) {
            Text(
                stringResource(R.string.cq_model_answer),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                sub.answerText(language) ?: stringResource(R.string.cq_no_model_answer),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Prefer the student's medium, fall back to the other language if a field is missing.
private fun CreativeQuestionDto.stemText(language: ContentLanguage): String = when (language) {
    ContentLanguage.BANGLA -> stemBn ?: stemEn
    ContentLanguage.ENGLISH -> stemEn ?: stemBn
} ?: ""

private fun SubQuestionDto.questionText(language: ContentLanguage): String = when (language) {
    ContentLanguage.BANGLA -> textBn ?: textEn
    ContentLanguage.ENGLISH -> textEn ?: textBn
} ?: ""

private fun SubQuestionDto.answerText(language: ContentLanguage): String? = when (language) {
    ContentLanguage.BANGLA -> modelAnswerBn ?: modelAnswerEn
    ContentLanguage.ENGLISH -> modelAnswerEn ?: modelAnswerBn
}

/** 3.0 -> "3", 2.5 -> "2.5". */
private fun formatMarks(marks: Double): String =
    if (marks % 1.0 == 0.0) marks.toInt().toString() else String.format(Locale.US, "%.1f", marks)
