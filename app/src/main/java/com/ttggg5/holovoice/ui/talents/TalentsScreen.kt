package com.ttggg5.holovoice.ui.talents

import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.LibraryRefreshBox
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.pinchToResize

/** One card on the Talents page: a roster member, with their clips in the library (if any). */
private data class TalentCardData(
    val slug: String?,
    val nameJa: String?,
    val nameEn: String,
    val imageUrl: String?,
    val clipCount: Int,
    /** Status tags taken off the name, e.g. "Alum". */
    val tags: List<String> = emptyList(),
)

/**
 * Every hololive member: the talents you have clips for first (in the chosen sort), then the rest of
 * the official roster. Cards show the picture with the Japanese and English names; tap one to see
 * their clips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentsScreen(viewModel: TalentsViewModel, playerState: PlayerState, onOpen: (String) -> Unit) {
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
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
        val mine = talents
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (mine == null) return@Column
            if (mine.isEmpty() && roster.isEmpty()) {
                EmptyMessage(
                    "No talents yet",
                    "Import some voice files first. Files named after a talent get their picture automatically. " +
                        "An internet connection is needed once to download the talent list.",
                    icon = Icons.Filled.Face,
                )
                return@Column
            }
            val bySlug = remember(roster) { roster.associateBy { it.slug } }
            val inLibrary = remember(mine, bySlug, sort) {
                sort.sorted(mine).map { t ->
                    val official = t.slug?.let(bySlug::get)
                    val (savedName, savedTags) = TalentTags.split(t.name.orEmpty())
                    TalentCardData(
                        slug = t.slug,
                        nameJa = official?.nameJa,
                        nameEn = official?.nameEn ?: savedName.ifBlank { "No talent yet" },
                        imageUrl = official?.imageUrl ?: t.imageUrl,
                        clipCount = t.clipCount,
                        tags = (official?.tags.orEmpty() + savedTags).distinct(),
                    )
                }
            }
            val others = remember(mine, roster) {
                val have = mine.mapNotNull { it.slug }.toSet()
                roster.filter { it.slug !in have }.map { TalentCardData(it.slug, it.nameJa, it.nameEn, it.imageUrl, 0, it.tags) }
            }
            fun TalentCardData.matches(): Boolean {
                if (query.isBlank()) return true
                val q = TalentMatcher.normalize(query)
                return (listOfNotNull(nameEn, nameJa) + tags).any {
                    it.contains(query.trim(), ignoreCase = true) || (q.isNotEmpty() && TalentMatcher.normalize(it).contains(q))
                }
            }
            val shownMine = inLibrary.filter { it.matches() }
            val shownOthers = others.filter { it.matches() }
            val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
            LibraryRefreshBox(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    // Portrait cards follow the same pinch size as clip tiles.
                    columns = GridCells.Adaptive(minSize = (cellDp * 0.8f).dp),
                    modifier = Modifier.pinchToResize(prefs),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Find a member (English or 日本語)") },
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
                    if (shownMine.isNotEmpty()) {
                        item(key = "mineHeader", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("In your library", shownMine.size)
                        }
                        items(shownMine, key = { "mine:" + (it.slug ?: UNASSIGNED_SLUG) }) { t ->
                            PortraitCard(
                                talent = t,
                                inLibrary = true,
                                playing = playerState.isPlaying && t.slug != null && t.slug in playerState.talentSlugs,
                                modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                                onClick = { onOpen(t.slug ?: UNASSIGNED_SLUG) },
                            )
                        }
                    }
                    if (shownOthers.isNotEmpty()) {
                        item(key = "othersHeader", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Other members", shownOthers.size)
                        }
                        items(shownOthers, key = { "other:" + it.slug }) { t ->
                            PortraitCard(
                                talent = t,
                                inLibrary = false,
                                playing = false,
                                modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                                onClick = { t.slug?.let(onOpen) },
                            )
                        }
                    }
                    if (shownMine.isEmpty() && shownOthers.isEmpty()) {
                        item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                            EmptyMessage("No members match", "Try another name.", icon = Icons.Filled.Search, fill = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "  $count",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A tall card: the talent's picture fills the top, then the Japanese name (large) and the English
 * name (small), and a clip count. Members not in the library are dimmed.
 */
@Composable
private fun PortraitCard(talent: TalentCardData, inLibrary: Boolean, playing: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (playing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (playing) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Artwork(
                talent.imageUrl,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (inLibrary) 1f else 0.55f },
                iconSize = 44.dp,
                placeholder = if (talent.slug == null) Icons.Filled.PersonOff else Icons.Filled.Face,
            )
            if (talent.tags.isNotEmpty()) {
                // Status tags like "Alum" sit on the picture instead of in the name.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    talent.tags.forEach { TagBadge(it) }
                }
            }
            if (playing) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    EqualizerBars(true, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp))
                }
            }
            if (inLibrary && talent.clipCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Text(
                        "${talent.clipCount} clips",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                talent.nameJa ?: talent.nameEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (inLibrary) 1f else 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (talent.nameJa != null) {
                Text(
                    talent.nameEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
