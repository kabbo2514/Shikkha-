package com.shikkha.app.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shikkha.app.R
import com.shikkha.app.auth.data.ProfileDto

/** A feature that exists in the master spec but is not implemented in this build. */
enum class DashboardDestination {
    Subjects, Practice, Mistakes, PlannedFeature, Profile, Settings
}

data class DashboardTile(
    val titleRes: Int,
    val icon: ImageVector,
    val destination: DashboardDestination,
    val planned: Boolean = false,
)

private val tiles = listOf(
    DashboardTile(R.string.tile_subjects, Icons.Default.Home, DashboardDestination.Subjects),
    DashboardTile(R.string.tile_mcq_practice, Icons.Default.List, DashboardDestination.Practice),
    DashboardTile(R.string.tile_exams, Icons.Default.DateRange, DashboardDestination.PlannedFeature, planned = true),
    DashboardTile(R.string.tile_mistakes, Icons.Default.Refresh, DashboardDestination.Mistakes),
    DashboardTile(R.string.tile_friends, Icons.Default.Face, DashboardDestination.PlannedFeature, planned = true),
    DashboardTile(R.string.tile_chat, Icons.Default.Notifications, DashboardDestination.PlannedFeature, planned = true),
    DashboardTile(R.string.tile_videos, Icons.Default.PlayArrow, DashboardDestination.PlannedFeature, planned = true),
    DashboardTile(R.string.tile_leaderboard, Icons.Default.Star, DashboardDestination.PlannedFeature, planned = true),
    DashboardTile(R.string.tile_profile, Icons.Default.AccountCircle, DashboardDestination.Profile),
    DashboardTile(R.string.tile_settings, Icons.Default.Settings, DashboardDestination.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: ProfileDto,
    onTileClick: (DashboardTile) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                GreetingHeader(profile)
            }
            items(tiles, key = { it.titleRes }) { tile ->
                DashboardTileCard(tile) { onTileClick(tile) }
            }
        }
    }
}

@Composable
private fun GreetingHeader(profile: ProfileDto) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_welcome, profile.fullName),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            R.string.dashboard_class_medium,
                            profile.classLevel,
                            if (profile.medium == "bangla") {
                                stringResource(R.string.medium_bangla)
                            } else {
                                stringResource(R.string.medium_english)
                            },
                        )
                    )
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_new_user),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DashboardTileCard(tile: DashboardTile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(110.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                tile.icon,
                contentDescription = null,
                tint = if (tile.planned) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(28.dp),
            )
            Text(stringResource(tile.titleRes), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Empty state for features whose implementation is PLANNED (Spec 1.5 / 3.3). */
@Composable
fun PlannedFeatureScreen(titleRes: Int, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.planned_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.planned_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
    }
}
