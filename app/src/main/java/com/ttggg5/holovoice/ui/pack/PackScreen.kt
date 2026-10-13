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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.ttggg5.holovoice.appContainer
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
import com.ttggg5.holovoice.ui.components.ClipBrowser

/** A topic: its clips, split into category tabs when the topic folder had sub-folders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackScreen(viewModel: PackViewModel, player: PlayerController, onBack: () -> Unit) {
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val allClips by viewModel.clips.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    // Page 0 = All, page i = categories[i - 1]; swipe left/right to change category.
    val pager = rememberPagerState { categories.size + 1 }
    val scope = rememberCoroutineScope()
    val tabSwipes by LocalContext.current.appContainer.uiPrefs.tabSwipes.state.collectAsStateWithLifecycle()
    val current = pager.currentPage.coerceAtMost(categories.size)
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
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            HorizontalPager(state = pager, key = { it }, userScrollEnabled = tabSwipes) { tab ->
                val tabCategory = categories.getOrNull(tab - 1)
                ClipBrowser(
                    clips = if (tabCategory == null) allClips else allClips.filter { it.clip.category == tabCategory },
                    playerState = playerState,
                    player = player,
                    onToggleFavorite = viewModel::toggleFavorite,
                    showPack = false,
                    showCategory = tabCategory == null && categories.isNotEmpty(),
                    emptyTitle = "No clips in this topic",
                    emptyBody = "Add audio files to the folder; they appear the next time you open the app.",
                )
            }
        }
    }
}
