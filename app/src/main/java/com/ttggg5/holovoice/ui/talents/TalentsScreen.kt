package com.ttggg5.holovoice.ui.talents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.LibraryRefreshBox
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.pinchToResize

/** Every talent in the library with their picture; tap one to see all their clips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentsScreen(viewModel: TalentsViewModel, playerState: PlayerState, onOpen: (String) -> Unit) {
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.talentSort.collectAsStateWithLifecycle()
    val undo = LocalUndo.current
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Talents") },
                actions = {
                    IconButton(onClick = { viewModel.matchTalents { undo?.show(it) } }, enabled = !busy) {
                        Icon(Icons.Filled.Face, contentDescription = "Match talents automatically")
                    }
                    SortMenu(TalentSort.entries, sort, { it.label }, prefs::setTalentSort)
                },
            )
        },
    ) { padding ->
        val all = talents
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                all == null -> Unit
                all.isEmpty() -> EmptyMessage(
                    "No talents yet",
                    "Import some voice files first. Files named after a talent get their picture automatically.",
                    icon = Icons.Filled.Face,
                )
                all.none { it.slug != null } -> EmptyMessage(
                    "No talents matched yet",
                    "Name each file after the talent (e.g. 兎田ぺこら.mp3 or Usada Pekora.mp3), then match again. " +
                        "An internet connection is needed once to download the talent list.",
                    icon = Icons.Filled.Face,
                    actionLabel = "Match talents",
                    onAction = { viewModel.matchTalents { undo?.show(it) } },
                )
                else -> {
                    val shown = sort.sorted(all).filter { t ->
                        query.isBlank() || TalentMatcher.normalize(t.name ?: "").contains(TalentMatcher.normalize(query)) ||
                            (t.name ?: "").contains(query.trim(), ignoreCase = true)
                    }
                    val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
                    LibraryRefreshBox(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        // Talent cards are smaller than clip tiles but follow the same pinch size.
                        columns = GridCells.Adaptive(minSize = (cellDp * 0.7f).dp),
                        modifier = Modifier.pinchToResize(prefs),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Find a talent") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            )
                        }
                        items(shown, key = { it.slug ?: UNASSIGNED_SLUG }) { t ->
                            TalentCard(
                                talent = t,
                                playing = playerState.isPlaying && t.slug != null && t.slug in playerState.talentSlugs,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpen(t.slug ?: UNASSIGNED_SLUG) },
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun TalentCard(talent: TalentSummary, playing: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        border = if (playing) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
        ) {
            androidx.compose.foundation.layout.Box {
                Artwork(
                    talent.imageUrl,
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    iconSize = 36.dp,
                    placeholder = if (talent.slug == null) Icons.Filled.PersonOff else Icons.Filled.Face,
                )
                if (playing) EqualizerBars(true, modifier = Modifier.align(Alignment.BottomEnd))
            }
            Text(
                talent.name ?: "No talent yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "${talent.clipCount} clips",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
