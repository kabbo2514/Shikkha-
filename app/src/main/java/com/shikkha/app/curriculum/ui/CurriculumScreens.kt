package com.shikkha.app.curriculum.ui

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shikkha.app.R
import com.shikkha.app.core.common.UiState
import com.shikkha.app.curriculum.data.ChapterDto
import com.shikkha.app.curriculum.data.CurriculumRepository
import com.shikkha.app.curriculum.data.SubjectDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: CurriculumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<SubjectDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<SubjectDto>>> = _uiState

    fun load(classLevel: Int, medium: String) {
        if (_uiState.value !is UiState.Loading) return
        viewModelScope.launch {
            _uiState.value = try {
                val subjects = repository.getSubjects(classLevel, medium)
                UiState.Success(subjects)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }

    fun reload(classLevel: Int, medium: String) {
        _uiState.value = UiState.Loading
        load(classLevel, medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    classLevel: Int,
    medium: String,
    onBack: () -> Unit,
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel(),
) {
    viewModel.load(classLevel, medium)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subjects_title)) },
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
                onRetry = { viewModel.reload(classLevel, medium) },
                modifier = Modifier.padding(padding),
            )

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.empty_subjects)) }
                } else {
                    LazyColumn(
                        contentPadding = padding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(s.data, key = { it.id }) { subject ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { onSubjectClick(subject.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(subject.nameEn, style = MaterialTheme.typography.titleMedium)
                                        Text(subject.nameBn, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val repository: CurriculumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Pair<ChapterDto, Long>>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Pair<ChapterDto, Long>>>> = _uiState

    fun load(subjectId: String) {
        if (_uiState.value !is UiState.Loading) return
        viewModelScope.launch {
            _uiState.value = try {
                val chapters = repository.getChapters(subjectId)
                val withCounts = chapters.map { it to repository.countPublishedQuestions(it.id) }
                UiState.Success(withCounts)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }

    fun reload(subjectId: String) {
        _uiState.value = UiState.Loading
        load(subjectId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    subjectId: String,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    onOpenCreativeQuestions: (String) -> Unit,
    viewModel: ChaptersViewModel = hiltViewModel(),
) {
    viewModel.load(subjectId)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chapters_title)) },
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
                onRetry = { viewModel.reload(subjectId) },
                modifier = Modifier.padding(padding),
            )

            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.empty_chapters)) }
                } else {
                    LazyColumn(
                        contentPadding = padding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(s.data, key = { it.first.id }) { (chapter, questionCount) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(chapter.nameEn, style = MaterialTheme.typography.titleMedium)
                                    Text(chapter.nameBn, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.questions_count, questionCount.toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        androidx.compose.material3.Button(
                                            onClick = { onStartPractice(chapter.id) },
                                            enabled = questionCount > 0,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.start_practice))
                                        }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { onOpenCreativeQuestions(chapter.id) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.cq_button))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageWithRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.try_again))
        }
    }
}
