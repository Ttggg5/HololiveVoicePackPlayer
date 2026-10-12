package com.ttggg5.holovoice.ui.pack

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.ClipGrid

/** A topic: its clips, split into category tabs when the topic folder had sub-folders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackScreen(viewModel: PackViewModel, player: PlayerController, onBack: () -> Unit) {
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val allClips by viewModel.clips.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    // 0 = All, i = categories[i - 1]
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val current = if (selected <= categories.size) selected else 0
    val category = categories.getOrNull(current - 1)
    val clips = if (category == null) allClips else allClips.filter { it.clip.category == category }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(pack?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${allClips.size} clips" + if (categories.isNotEmpty()) " · ${categories.size} categories" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { player.playQueue(clips, shuffle = false) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play all")
                    }
                    IconButton(onClick = { player.playQueue(clips, shuffle = true) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AnimatedVisibility(
                visible = categories.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ScrollableTabRow(selectedTabIndex = current, edgePadding = 12.dp) {
                    (listOf("All") + categories).forEachIndexed { index, title ->
                        Tab(
                            selected = current == index,
                            onClick = { selected = index },
                            text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            // The grid slides in from the side of the tab that was picked.
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(250)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
                },
                label = "categoryTab",
            ) { tab ->
                val tabCategory = categories.getOrNull(tab - 1)
                ClipGrid(
                    clips = if (tabCategory == null) allClips else allClips.filter { it.clip.category == tabCategory },
                    playerState = playerState,
                    showPack = false,
                    showCategory = tabCategory == null && categories.isNotEmpty(),
                    player = player,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}
