package com.ttggg5.holovoice.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.ClipBrowser
import com.ttggg5.holovoice.ui.playlists.formatTotal
import com.ttggg5.holovoice.ui.talents.StatTile

/**
 * Starred clips: a header (gradient banner, star art, stats, play buttons and talent filters), then
 * the clips themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel, player: PlayerController, onBack: () -> Unit) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    var talentFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val all = favorites.orEmpty()
    // Talents among the favorites, the ones with the most favorites first.
    val talents = all.flatMap { it.orderedTalents }
        .groupBy { it.slug }
        .values.sortedByDescending { it.size }
        .map { it.first() }
    val selected = talentFilter?.takeIf { slug -> talents.any { it.slug == slug } }
    val clips = if (selected == null) all else all.filter { c -> c.orderedTalents.any { it.slug == selected } }

    // The top bar is see-through over the banner and fills in once the list scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (scrollBehavior.state.overlappedFraction > 0.5f) Text("Favorites")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (favorites != null) {
            ClipBrowser(
                clips = clips,
                playerState = playerState,
                player = player,
                onToggleFavorite = viewModel::toggleFavorite,
                // The header starts under the top bar so the banner reaches the top of the screen.
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                emptyTitle = "No favorites yet",
                emptyBody = "Tap the star on any clip to collect it here.",
                header = {
                    FavoritesHeader(
                        cover = all.firstNotNullOfOrNull { it.coverUri },
                        clipCount = all.size,
                        talentCount = talents.size,
                        topicCount = all.distinctBy { it.clip.packId }.size,
                        totalMs = all.sumOf { it.clip.durationMs ?: 0L },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Button(onClick = { player.playQueue(clips, shuffle = false) }, enabled = clips.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(" Play all")
                            }
                            FilledTonalButton(onClick = { player.playQueue(clips, shuffle = true) }, enabled = clips.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Text(" Shuffle")
                            }
                        }
                        if (talents.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            ) {
                                FilterChip(selected = selected == null, onClick = { talentFilter = null }, label = { Text("All") })
                                talents.forEach { t -> TalentChip(t, selected == t.slug) { talentFilter = if (selected == t.slug) null else t.slug } }
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TalentChip(talent: ClipTalent, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(TalentTags.clean(talent.name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Artwork(talent.imageUrl, Modifier.size(22.dp).clip(CircleShape), iconSize = 12.dp) },
    )
}

@Composable
private fun FavoritesHeader(
    cover: String?,
    clipCount: Int,
    talentCount: Int,
    topicCount: Int,
    totalMs: Long,
    actions: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    // Reaches past the list's side and top padding so the banner runs edge to edge.
    Box(
        Modifier.fillMaxWidth().layout { measurable, constraints ->
            val side = 12.dp.roundToPx()
            val top = 4.dp.roundToPx()
            val wide = constraints.maxWidth + side * 2
            val placeable = measurable.measure(constraints.copy(minWidth = wide, maxWidth = wide))
            layout(constraints.maxWidth, (placeable.height - top).coerceAtLeast(0)) { placeable.place(-side, -top) }
        },
    ) {
        // Banner: the Favorites colours with a favorite's cover blurred into them, fading into the page.
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Brush.linearGradient(listOf(secondary.copy(alpha = 0.55f), tertiary.copy(alpha = 0.55f)))),
        ) {
            BlurredCover(
                model = cover,
                wash = Brush.verticalGradient(
                    0f to surface.copy(alpha = 0.45f),
                    0.6f to surface.copy(alpha = 0.6f),
                    1f to surface,
                ),
                modifier = Modifier.fillMaxSize(),
                blur = 6.dp,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 104.dp)) {
            Box(
                Modifier
                    .size(150.dp)
                    .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = secondary, spotColor = secondary)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(secondary, tertiary))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(76.dp))
            }
            Text(
                "Favorites",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
            ) {
                StatTile("$clipCount", "clips", Modifier.weight(1f))
                StatTile("$talentCount", "talents", Modifier.weight(1f))
                StatTile("$topicCount", "topics", Modifier.weight(1f))
                formatTotal(totalMs).takeIf { it.isNotEmpty() }?.let { StatTile(it, "total", Modifier.weight(1f)) }
            }
            actions()
        }
    }
}
