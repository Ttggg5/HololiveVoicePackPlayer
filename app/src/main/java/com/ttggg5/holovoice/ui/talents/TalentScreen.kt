package com.ttggg5.holovoice.ui.talents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.ClipArt
import com.ttggg5.holovoice.ui.components.ClipBrowser

/**
 * One talent: a profile header (blurred banner, ringed picture, Japanese and English names, status
 * tags such as "Alum", stats, play buttons and topic filters), then all their clips across topics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(viewModel: TalentViewModel, player: PlayerController, onBack: () -> Unit) {
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val talent by viewModel.talent.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    var topicFilter by rememberSaveable { mutableStateOf<Long?>(null) }

    val all = clips.orEmpty()
    val topics = all.map { Triple(it.clip.packId, it.packName, it.coverUri) }.distinctBy { it.first }
    val shown = if (topicFilter == null) all else all.filter { it.clip.packId == topicFilter }
    // Names and tags: the official list first, then the name saved with the clips.
    val (savedName, savedTags) = TalentTags.split(summary?.name.orEmpty())
    val nameEn = talent?.nameEn ?: savedName.ifBlank { if (viewModel.slug == null) "No talent yet" else "" }
    val nameJa = talent?.nameJa
    val tags = (talent?.tags.orEmpty() + savedTags).distinct()
    val picture = talent?.imageUrl ?: summary?.imageUrl

    // The top bar is see-through over the banner and fills in once the list scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (scrollBehavior.state.overlappedFraction > 0.5f) {
                        Text(nameJa ?: nameEn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
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
        if (clips != null) {
            ClipBrowser(
                clips = shown,
                playerState = playerState,
                player = player,
                onToggleFavorite = viewModel::toggleFavorite,
                // The header starts under the top bar so the banner reaches the top of the screen.
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                emptyTitle = "No clips of ${nameJa ?: nameEn} yet",
                emptyBody = "Files named after them (in English or 日本語) are matched automatically when you import or refresh.",
                // All clips share this talent's picture, so show each clip's topic thumbnail instead.
                artwork = ClipArt.TOPIC,
                header = {
                    TalentHeader(
                        picture = picture,
                        nameJa = nameJa,
                        nameEn = nameEn,
                        tags = tags,
                        unassigned = viewModel.slug == null,
                        clipCount = all.size,
                        topicCount = topics.size,
                        plays = summary?.plays ?: 0,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Button(onClick = { player.playQueue(shown, shuffle = false) }, enabled = shown.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(" Play all")
                            }
                            FilledTonalButton(onClick = { player.playQueue(shown, shuffle = true) }, enabled = shown.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Text(" Shuffle")
                            }
                        }
                        if (topics.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            ) {
                                FilterChip(selected = topicFilter == null, onClick = { topicFilter = null }, label = { Text("All topics") })
                                topics.forEach { (id, topicName, cover) ->
                                    FilterChip(
                                        selected = topicFilter == id,
                                        onClick = { topicFilter = if (topicFilter == id) null else id },
                                        label = { Text(topicName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingIcon = {
                                            Artwork(cover, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)), iconSize = 12.dp)
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TalentHeader(
    picture: String?,
    nameJa: String?,
    nameEn: String,
    tags: List<String>,
    unassigned: Boolean,
    clipCount: Int,
    topicCount: Int,
    plays: Int,
    actions: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
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
        // Banner: the talent's picture, softly blurred, fading into the page.
        BlurredCover(
            model = picture,
            wash = Brush.verticalGradient(
                0f to surface.copy(alpha = 0.35f),
                0.6f to surface.copy(alpha = 0.55f),
                1f to surface,
            ),
            modifier = Modifier.fillMaxWidth().height(230.dp).background(MaterialTheme.colorScheme.surfaceContainer),
            blur = 4.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 120.dp)) {
            Box(
                Modifier
                    .size(132.dp)
                    .shadow(16.dp, CircleShape, ambientColor = primary, spotColor = primary)
                    .background(surface, CircleShape)
                    .border(4.dp, Brush.sweepGradient(listOf(primary, secondary, primary)), CircleShape)
                    .padding(7.dp),
            ) {
                Artwork(
                    picture,
                    Modifier.fillMaxWidth().height(118.dp).clip(CircleShape),
                    iconSize = 56.dp,
                    placeholder = if (unassigned) Icons.Filled.PersonOff else Icons.Filled.Face,
                )
            }
            Text(
                nameJa ?: nameEn,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )
            if (nameJa != null) {
                Text(
                    nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    tags.forEach { TagBadge(it) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            ) {
                StatTile("$clipCount", "clips", Modifier.weight(1f))
                StatTile("$topicCount", "topics", Modifier.weight(1f))
                StatTile("$plays", "plays", Modifier.weight(1f))
            }
            actions()
        }
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A small status label taken off a name, e.g. "Alum". */
@Composable
fun TagBadge(tag: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Text(
            tag,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
