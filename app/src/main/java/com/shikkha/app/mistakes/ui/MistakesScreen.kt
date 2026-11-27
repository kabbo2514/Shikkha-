package com.shikkha.app.mistakes.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shikkha.app.R
import com.shikkha.app.auth.data.AuthRepository
import com.shikkha.app.core.common.UiState
import com.shikkha.app.curriculum.ui.MessageWithRetry
import com.shikkha.app.mcq.data.McqDto
import com.shikkha.app.mcq.ui.ContentLanguage
import com.shikkha.app.mistakes.data.MistakeItem
import com.shikkha.app.mistakes.data.MistakesRepository
import com.shikkha.app.theme.CorrectGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MistakesContent(
    val items: List<MistakeItem>,
    val language: ContentLanguage,
)

@HiltViewModel
class MistakesViewModel @Inject constructor(
    private val repository: MistakesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<MistakesContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<MistakesContent>> = _uiState

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val medium = authRepository.currentProfile()?.medium ?: "bangla"
                val language = if (medium == "english") ContentLanguage.ENGLISH else ContentLanguage.BANGLA
                UiState.Success(MistakesContent(repository.getUnresolved(), language))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }
}

/**
 * Mistake library (Spec 8.2): every question the student got wrong and has not yet
 * answered correctly in a retry. Tap a card to read the explanation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    onBack: () -> Unit,
    onPracticeMistakes: () -> Unit,
    viewModel: MistakesViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mistakes_title)) },
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
                if (s.data.items.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.mistakes_empty),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    MistakesList(s.data, onPracticeMistakes, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun MistakesList(
    content: MistakesContent,
    onPracticeMistakes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.mistakes_count, content.items.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onPracticeMistakes, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mistakes_practice_button))
                }
            }
        }
        items(content.items, key = { it.question.id }) { item ->
            MistakeCard(item, content.language)
        }
    }
}

@Composable
private fun MistakeCard(item: MistakeItem, language: ContentLanguage) {
    var expanded by remember { mutableStateOf(false) }
    val question = item.question

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(question.questionText(language), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.correct_answer) + ":",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    question.correctAnswerText(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorrectGreen,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.mistakes_times_missed, item.repetitionCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.explanation_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    question.explanationText(language)
                        ?: stringResource(R.string.mistakes_no_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// Prefer the student's medium, fall back to the other language if a field is missing.
private fun McqDto.questionText(language: ContentLanguage): String = when (language) {
    ContentLanguage.BANGLA -> questionBn ?: questionEn
    ContentLanguage.ENGLISH -> questionEn ?: questionBn
} ?: ""

private fun McqDto.correctAnswerText(language: ContentLanguage): String {
    val option = options.getOrNull(correctIndex) ?: return ""
    return when (language) {
        ContentLanguage.BANGLA -> option.textBn ?: option.textEn
        ContentLanguage.ENGLISH -> option.textEn ?: option.textBn
    } ?: ""
}

private fun McqDto.explanationText(language: ContentLanguage): String? = when (language) {
    ContentLanguage.BANGLA -> explanationBn ?: explanationEn
    ContentLanguage.ENGLISH -> explanationEn ?: explanationBn
}
