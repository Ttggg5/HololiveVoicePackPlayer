package com.ttggg5.holovoice.ui.talents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.TalentSort
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LibraryRefreshBox
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.pinchToResize
import com.ttggg5.holovoice.ui.theme.Motion

/** One talent on the Talents page: a roster member, with their clips in the library (if any). */
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
 * Every hololive member: a headline with counts, a search bar, who is speaking now, the talents you
 * have clips for as photo cards (in the chosen sort), then the rest of the official roster as small
 * round pictures. Tap one to see their clips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TalentsScreen(viewModel: TalentsViewModel, playerState: PlayerState, onOpen: (String) -> Unit) {
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.talentSort.collectAsStateWithLifecycle()
    val undo = LocalUndo.current
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        val mine = talents
        Column(Modifier.padding(padding).fillMaxSize().statusBarsPadding()) {
            TalentsHeader(
                inLibrary = mine?.count { it.slug != null } ?: 0,
                members = roster.size,
                sort = sort,
                onSort = prefs::setTalentSort,
                busy = busy,
                onMatch = { viewModel.matchTalents { undo?.show(it) } },
            )
            AnimatedVisibility(visible = busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            }
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
            // The first speaking talent that is in the library.
            val speaking = playerState.talentSlugs.firstNotNullOfOrNull { slug -> inLibrary.firstOrNull { it.slug == slug } }
                ?.takeIf { playerState.clipId != null }
            val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
            LibraryRefreshBox(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    // Photo cards follow the same pinch size as clip tiles.
                    columns = GridCells.Adaptive(minSize = (cellDp * 0.8f).dp),
                    modifier = Modifier.pinchToResize(prefs),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                        TalentSearchField(query) { query = it }
                    }
                    if (speaking != null) {
                        item(key = "speaking", span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                                NowSpeakingStrip(speaking, playerState.isPlaying) { onOpen(speaking.slug ?: UNASSIGNED_SLUG) }
                            }
                        }
                    }
                    if (shownMine.isNotEmpty()) {
                        item(key = "mineHeader", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("In your library", shownMine.size)
                        }
                        items(shownMine, key = { "mine:" + (it.slug ?: UNASSIGNED_SLUG) }) { t ->
                            TalentPhotoCard(
                                talent = t,
                                speaking = t.slug != null && t.slug in playerState.talentSlugs && playerState.clipId != null,
                                isPlaying = playerState.isPlaying,
                                modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                                onClick = { onOpen(t.slug ?: UNASSIGNED_SLUG) },
                            )
                        }
                    }
                    if (shownOthers.isNotEmpty()) {
                        item(key = "othersHeader", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Other members", shownOthers.size)
                        }
                        // Smaller cells than the photo cards, so the whole roster fits in a few rows.
                        item(key = "others", span = { GridItemSpan(maxLineSpan) }) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                shownOthers.forEach { t -> RosterAvatar(t) { t.slug?.let(onOpen) } }
                            }
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
private fun TalentsHeader(
    inLibrary: Int,
    members: Int,
    sort: TalentSort,
    onSort: (TalentSort) -> Unit,
    busy: Boolean,
    onMatch: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 4.dp, bottom = 8.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Talents", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            val counts = listOfNotNull(
                "$inLibrary in your library",
                members.takeIf { it > 0 }?.let { "$it members" },
            )
            Text(counts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onMatch, enabled = !busy) {
            Icon(Icons.Filled.Face, contentDescription = "Match talents automatically")
        }
        SortMenu(TalentSort.entries, sort, { it.label }, onSort)
    }
}

@Composable
private fun TalentSearchField(query: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Find a member (English or 日本語)") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "  $count",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Slim card for the talent speaking in the current clip; tap to open their page. */
@Composable
private fun NowSpeakingStrip(talent: TalentCardData, isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        BlurredCover(
            model = talent.imageUrl,
            wash = SolidColor(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            modifier = Modifier.matchParentSize(),
            blur = 3.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Artwork(
                talent.imageUrl,
                Modifier.size(48.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                iconSize = 22.dp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(isPlaying, size = 14.dp)
                    Text(
                        "  Now speaking",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    listOfNotNull(talent.nameJa, talent.nameEn).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** A talent in the library: their picture full-bleed with the names and clip count on a dark gradient. */
@Composable
private fun TalentPhotoCard(talent: TalentCardData, speaking: Boolean, isPlaying: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "talentPress")
    val ring by animateColorAsState(
        if (speaking) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(Motion.MEDIUM),
        label = "talentRing",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .border(3.dp, ring, shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick),
    ) {
        Artwork(
            talent.imageUrl,
            Modifier.fillMaxSize(),
            iconSize = 48.dp,
            placeholder = if (talent.slug == null) Icons.Filled.PersonOff else Icons.Filled.Face,
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
        if (talent.tags.isNotEmpty()) {
            // Status tags like "Alum" sit on the picture instead of in the name.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                talent.tags.forEach { TagBadge(it) }
            }
        }
        AnimatedVisibility(visible = speaking, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                EqualizerBars(isPlaying, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Text(
                talent.nameJa ?: talent.nameEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (talent.nameJa != null) talent.nameEn else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (talent.clipCount > 0) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.22f)) {
                        Text(
                            "${talent.clipCount} clips",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A roster member without clips yet: a small dimmed round picture and their name. */
@Composable
private fun RosterAvatar(talent: TalentCardData, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Box {
            Artwork(talent.imageUrl, Modifier.size(64.dp).clip(CircleShape).alpha(0.55f), iconSize = 28.dp, placeholder = Icons.Filled.Face)
            talent.tags.firstOrNull()?.let { TagBadge(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Text(
            talent.nameJa ?: talent.nameEn,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
