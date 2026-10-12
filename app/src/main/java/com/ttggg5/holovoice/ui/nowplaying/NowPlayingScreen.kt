package com.ttggg5.holovoice.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.AddToPlaylistDialog
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.formatDuration
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val SLEEP_MINUTES = listOf(5, 15, 30, 45, 60, 90)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    player: PlayerController,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val state by player.state.collectAsStateWithLifecycle()
    val clip by viewModel.currentClip.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Now playing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                    }
                },
            )
        },
    ) { padding ->
        if (state.clipId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nothing is playing", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Artwork(
                    state.artworkUri,
                    Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)),
                    iconSize = 96.dp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp)) {
                IconButton(onClick = { sheet = Sheet.Playlist }, enabled = clip != null) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(state.subtitle, state.album).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val favorite = clip?.clip?.isFavorite == true
                IconButton(onClick = { clip?.let(viewModel::toggleFavorite) }, enabled = clip != null) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favorite) "Remove favorite" else "Add favorite",
                        tint = if (favorite) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                    )
                }
            }
            SeekBar(state, player)
            Transport(state, player)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                TextButton(onClick = { sheet = Sheet.Speed }) {
                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${formatSpeed(state.speed)}")
                }
                TextButton(onClick = { sheet = Sheet.Sleep }) {
                    Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${sleepLabel(state)}")
                }
            }
        }
    }

    when (sheet) {
        Sheet.Speed -> OptionSheet(onDismiss = { sheet = null }, title = "Playback speed") {
            SPEEDS.forEach { speed ->
                OptionRow(formatSpeed(speed), selected = state.speed == speed) {
                    player.setSpeed(speed)
                    sheet = null
                }
            }
        }
        Sheet.Sleep -> OptionSheet(onDismiss = { sheet = null }, title = "Sleep timer") {
            OptionRow("Off", selected = state.sleepEndsAt == 0L && !state.sleepAtEndOfTrack) {
                player.setSleepTimer()
                sheet = null
            }
            OptionRow("End of current clip", selected = state.sleepAtEndOfTrack) {
                player.setSleepTimer(endOfTrack = true)
                sheet = null
            }
            SLEEP_MINUTES.forEach { minutes ->
                OptionRow("$minutes minutes", selected = false) {
                    player.setSleepTimer(minutes = minutes)
                    sheet = null
                }
            }
        }
        Sheet.Playlist -> clip?.let { AddToPlaylistDialog(listOf(it.clip.id), onDismiss = { sheet = null }) }
        null -> Unit
    }
}

private enum class Sheet { Speed, Sleep, Playlist }

@Composable
private fun SeekBar(state: PlayerState, player: PlayerController) {
    val position by player.position.collectAsStateWithLifecycle(initialValue = 0L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val shown = if (dragging) dragValue.toLong() else position.coerceIn(0L, duration.coerceAtLeast(0L))

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Slider(
            value = shown.toFloat(),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                player.seekTo(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            enabled = duration > 0,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shown), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Transport(state: PlayerState, player: PlayerController) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = LocalContentColor.current.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        IconButton(onClick = player::toggleShuffle) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (state.shuffle) active else inactive)
        }
        IconButton(onClick = player::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = player::togglePlayPause, modifier = Modifier.size(72.dp)) {
            Icon(
                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = player::next, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = player::cycleRepeat) {
            Icon(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) inactive else active,
            )
        }
    }
}

@Composable
private fun sleepLabel(state: PlayerState): String {
    if (state.sleepAtEndOfTrack) return "End of clip"
    if (state.sleepEndsAt == 0L) return "Sleep"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.sleepEndsAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return formatDuration((state.sleepEndsAt - now).coerceAtLeast(0L))
}

private fun formatSpeed(speed: Float): String = if (speed % 1f == 0f) "%.1f×".format(speed) else "$speed×"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(onDismiss: () -> Unit, title: String, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
