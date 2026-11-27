package com.shikkha.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shikkha.app.auth.data.AuthRepository
import com.shikkha.app.auth.data.ProfileDto
import com.shikkha.app.core.common.UiState
import com.shikkha.app.mcq.data.PracticeAttemptDto
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileStats(
    val profile: ProfileDto,
    val attempts: Int,
    val correctAnswers: Int,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val client: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProfileStats>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileStats>> = _uiState

    var editingName by androidx.compose.runtime.mutableStateOf(false)
    var nameDraft by androidx.compose.runtime.mutableStateOf("")
    var saveError by androidx.compose.runtime.mutableStateOf(false)
        private set

    fun load() {
        if (_uiState.value !is UiState.Loading) return
        viewModelScope.launch {
            _uiState.value = try {
                val profile = authRepository.currentProfile()
                    ?: throw IllegalStateException("no profile")
                val attempts = client.from("practice_attempts")
                    .select { filter { eq("student_id", profile.id) } }
                    .decodeList<PracticeAttemptDto>()
                UiState.Success(
                    ProfileStats(
                        profile = profile,
                        attempts = attempts.size,
                        correctAnswers = attempts.sumOf { it.correctCount },
                    )
                )
            } catch (e: Exception) {
                UiState.Error(e.message ?: "load failed")
            }
        }
    }

    fun reload() {
        _uiState.value = UiState.Loading
        load()
    }

    fun beginEditName(current: String) {
        nameDraft = current
        editingName = true
    }

    fun saveName(onSaved: (ProfileDto) -> Unit) {
        viewModelScope.launch {
            try {
                val uid = client.auth.currentUserOrNull()?.id ?: return@launch
                client.from("profiles").update(
                    { set("full_name", nameDraft.trim()) }
                ) {
                    filter { eq("id", uid) }
                }
                editingName = false
                saveError = false
                val updated = authRepository.currentProfile()
                if (updated != null) {
                    _uiState.value = UiState.Success(
                        (_uiState.value as? UiState.Success)?.data?.copy(profile = updated)
                            ?: ProfileStats(updated, 0, 0)
                    )
                    onSaved(updated)
                }
            } catch (e: Exception) {
                saveError = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onProfileUpdated: (ProfileDto) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
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

            is UiState.Error -> com.shikkha.app.curriculum.ui.MessageWithRetry(
                message = stringResource(R.string.error_generic),
                onRetry = { viewModel.reload() },
                modifier = Modifier.padding(padding),
            )

            is UiState.Success -> ProfileContent(
                stats = s.data,
                viewModel = viewModel,
                onProfileUpdated = onProfileUpdated,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    stats: ProfileStats,
    viewModel: ProfileViewModel,
    onProfileUpdated: (ProfileDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (viewModel.editingName) {
        AlertDialog(
            onDismissRequest = { viewModel.editingName = false },
            title = { Text(stringResource(R.string.profile_edit_name)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.nameDraft,
                        onValueChange = { viewModel.nameDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (viewModel.saveError) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.profile_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveName(onProfileUpdated) }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.editingName = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stats.profile.fullName, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "@${stats.profile.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        R.string.dashboard_class_medium,
                        stats.profile.classLevel,
                        if (stats.profile.medium == "bangla") {
                            stringResource(R.string.medium_bangla)
                        } else {
                            stringResource(R.string.medium_english)
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                stats.profile.createdAt?.take(10)?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.profile_member_since, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.beginEditName(stats.profile.fullName) }) {
                    Text(stringResource(R.string.profile_edit_name))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.stat_attempts),
                value = stats.attempts.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_correct),
                value = stats.correctAnswers.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
