package com.ttggg5.holovoice.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Loop
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.ui.components.LocalIsWideLayout
import com.ttggg5.holovoice.ui.queue.QueueList
import com.ttggg5.holovoice.ui.components.PlayPauseIcon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    onOpenTopic: (Long) -> Unit,
    onOpenTalent: (String) -> Unit,
) {
    val state by player.state.collectAsStateWithLifecycle()
    val clip by viewModel.currentClip.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    val uiPrefs = LocalContext.current.appContainer.uiPrefs
    val skipBack by uiPrefs.skipBackSec.state.collectAsStateWithLifecycle()
    val skipForward by uiPrefs.skipForwardSec.state.collectAsStateWithLifecycle()

    // Drag the whole screen down to minimize it back into the mini player.
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    var screenHeight by remember { mutableIntStateOf(1) }
    val prefs = LocalContext.current.appContainer.uiPrefs
    val playerSwipes by prefs.playerSwipes.state.collectAsStateWithLifecycle()
    val wide = LocalIsWideLayout.current
    var showQueue by remember { mutableStateOf(false) }
    // Upward drag distance; swiping up far enough opens the queue (phones only).
    var upDrag by remember { mutableFloatStateOf(0f) }
    val upThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    val dismissDrag = rememberDraggableState { delta ->
        if (delta < 0f && dragY.value <= 0f) {
            upDrag += delta
        } else {
            scope.launch { dragY.snapTo((dragY.value + delta).coerceAtLeast(0f)) }
        }
    }
    Scaffold(
        modifier = Modifier
            .onSizeChanged { screenHeight = it.height }
            .draggable(
                state = dismissDrag,
                orientation = Orientation.Vertical,
                enabled = playerSwipes,
                onDragStarted = { upDrag = 0f },
                onDragStopped = { velocity ->
                    if (!wide && dragY.value <= 0f && (upDrag < -upThreshold || velocity < -2000f)) {
                        upDrag = 0f
                        showQueue = true
                    } else if (dragY.value > screenHeight * 0.25f || velocity > 2500f) {
                        onBack()
                    } else {
                        dragY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                },
            )
            .graphicsLayer {
                translationY = dragY.value
                val progress = (dragY.value / screenHeight).coerceIn(0f, 1f)
                scaleX = 1f - progress * 0.08f
                scaleY = 1f - progress * 0.08f
                alpha = 1f - progress * 0.5f
            },
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
                    if (!wide) {
                        IconButton(onClick = { showQueue = true }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                        }
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
        // Phones: one column. Tablets/unfolded foldables: player on the left, queue on the right.
        Row(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            // Topic cover, with the talent's picture as a badge; slides when the clip changes,
            // and breathes down slightly while paused.
            val artScale by animateFloatAsState(
                targetValue = if (state.isPlaying) 1f else 0.88f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "artScale",
            )
            AnimatedContent(
                targetState = state,
                contentKey = { it.clipId },
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(250)))
                },
                label = "nowPlayingArt",
                modifier = Modifier.weight(1f).fillMaxWidth().artworkSwipe(player, state.hasNext),
            ) { s ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                            },
                    ) {
                        Artwork(
                            s.coverUri ?: s.artworkUri ?: s.talentImageUrl,
                            Modifier.fillMaxSize().padding(bottom = 28.dp).clip(RoundedCornerShape(20.dp)),
                            iconSize = 96.dp,
                        )
                        if (s.talentImageUrl != null && s.coverUri != null) {
                            // Round badge: the talent's picture, or a split collage for several talents.
                            Artwork(
                                if (s.talentImages.size > 1) s.artworkUri else s.talentImageUrl,
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(72.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                iconSize = 32.dp,
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                IconButton(onClick = { sheet = Sheet.Playlist }, enabled = clip != null) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                AnimatedContent(
                    targetState = state,
                    contentKey = { it.clipId },
                    transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                    label = "nowPlayingTitle",
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                ) { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            s.title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            if (s.subtitle.isNotEmpty()) {
                                var talentMenu by remember { mutableStateOf(false) }
                                val names = s.subtitle.split(" & ")
                                Box {
                                DropdownMenu(expanded = talentMenu, onDismissRequest = { talentMenu = false }) {
                                    s.talentSlugs.forEachIndexed { i, slug ->
                                        DropdownMenuItem(
                                            text = { Text(names.getOrElse(i) { slug }) },
                                            leadingIcon = {
                                                Artwork(s.talentImages.getOrNull(i), Modifier.size(28.dp).clip(CircleShape), iconSize = 16.dp)
                                            },
                                            onClick = {
                                                talentMenu = false
                                                onOpenTalent(slug)
                                            },
                                        )
                                    }
                                }
                                AssistChip(
                                    onClick = {
                                        when (s.talentSlugs.size) {
                                            0 -> Unit
                                            1 -> onOpenTalent(s.talentSlugs.first())
                                            else -> talentMenu = true
                                        }
                                    },
                                    label = { Text(s.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = {
                                        if (s.talentImageUrl != null) {
                                            Artwork(s.talentImageUrl, Modifier.size(20.dp).clip(CircleShape), iconSize = 12.dp)
                                        } else {
                                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    modifier = Modifier.widthIn(max = 180.dp),
                                )
                                }
                            }
                            if (s.album.isNotEmpty()) {
                                AssistChip(
                                    onClick = { s.packId?.let(onOpenTopic) },
                                    label = {
                                        Text(
                                            listOfNotNull(s.album, s.category).joinToString(" › "),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.widthIn(max = 200.dp),
                                )
                            }
                        }
                    }
                }
                val favorite = clip?.clip?.isFavorite == true
                IconButton(onClick = { clip?.let(viewModel::toggleFavorite) }, enabled = clip != null) {
                    AnimatedContent(
                        targetState = favorite,
                        transitionSpec = { scaleIn(initialScale = 0.4f) togetherWith scaleOut(targetScale = 0.4f) },
                        label = "favorite",
                    ) { fav ->
                        Icon(
                            if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (fav) "Remove favorite" else "Add favorite",
                            tint = if (fav) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                        )
                    }
                }
            }
            SeekBar(state, player)
            Transport(
                state,
                player,
                skipBackSec = skipBack,
                skipForwardSec = skipForward,
                onPickSkipBack = { sheet = Sheet.SkipBack },
                onPickSkipForward = { sheet = Sheet.SkipForward },
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                val active = MaterialTheme.colorScheme.primary
                val inactive = LocalContentColor.current.copy(alpha = 0.5f)
                IconButton(onClick = player::toggleShuffle) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (state.shuffle) active else inactive)
                }
                TextButton(onClick = { sheet = Sheet.Speed }) {
                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${formatSpeed(state.speed)}")
                }
                TextButton(onClick = { sheet = Sheet.Sleep }) {
                    Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${sleepLabel(state)}")
                }
                LoopButton(state, player, active, inactive)
                IconButton(onClick = player::cycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) inactive else active,
                    )
                }
            }
            if (!wide) {
                UpNextHandle(state, onClick = { showQueue = true })
            }
        }
        if (wide) {
            VerticalDivider()
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    "Up next",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                QueueList(player, Modifier.fillMaxSize())
            }
        }
        }
    }

    if (showQueue && !wide) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                "Up next",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
            QueueList(player, Modifier.fillMaxHeight(0.85f))
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
        Sheet.SkipBack -> OptionSheet(onDismiss = { sheet = null }, title = "Rewind by") {
            SKIP_SECONDS.forEach { sec ->
                OptionRow("$sec seconds", selected = skipBack == sec) {
                    uiPrefs.skipBackSec.set(sec)
                    sheet = null
                }
            }
        }
        Sheet.SkipForward -> OptionSheet(onDismiss = { sheet = null }, title = "Skip ahead by") {
            SKIP_SECONDS.forEach { sec ->
                OptionRow("$sec seconds", selected = skipForward == sec) {
                    uiPrefs.skipForwardSec.set(sec)
                    sheet = null
                }
            }
        }
        Sheet.Playlist -> clip?.let { AddToPlaylistDialog(listOf(it.clip.id), onDismiss = { sheet = null }) }
        null -> Unit
    }
}

private enum class Sheet { Speed, Sleep, Playlist, SkipBack, SkipForward }

/** Seconds offered for the rewind / skip-ahead buttons (also used by Settings). */
val SKIP_SECONDS = listOf(5, 10, 15, 30, 60)

/** "Up next" bar at the bottom of the phone player; tap (or swipe up anywhere) to open the queue. */
@Composable
private fun UpNextHandle(state: PlayerState, onClick: () -> Unit) {
    val position = state.queue.indexOfFirst { it.index == state.currentIndex }
    val next = state.queue.getOrNull(position + 1)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
        Text(
            if (next != null) "Up next: ${next.title}" else "Queue",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Swipe the artwork left/right for next/previous; it follows the finger and springs back. */
@Composable
private fun Modifier.artworkSwipe(player: PlayerController, hasNext: Boolean): Modifier {
    val enabled by LocalContext.current.appContainer.uiPrefs.playerSwipes.state.collectAsStateWithLifecycle()
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offset = remember { Animatable(0f) }
    val canNext by rememberUpdatedState(hasNext)
    return this
        .pointerInput(player) {
            val width = size.width.toFloat()
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        val x = offset.value
                        if (x < -width * 0.2f && canNext) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            player.next()
                        } else if (x > width * 0.2f) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            player.previous()
                        }
                        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f) } },
            ) { change, amount ->
                change.consume()
                scope.launch { offset.snapTo(offset.value + amount) }
            }
        }
        .graphicsLayer {
            translationX = offset.value
            rotationZ = offset.value / size.width.coerceAtLeast(1f) * 6f
        }
}

/**
 * A–B loop: first tap marks A at the current position, the second marks B and starts the loop,
 * the third clears it. A pending A is forgotten when the clip changes.
 */
@Composable
private fun LoopButton(state: PlayerState, player: PlayerController, active: Color, inactive: Color) {
    var pendingA by remember(state.clipId) { mutableStateOf<Long?>(null) }
    val looping = state.loopEndMs >= 0
    TextButton(
        onClick = {
            val now = player.currentPositionMs()
            val a = pendingA
            when {
                looping -> player.clearLoop()
                a == null -> pendingA = now
                else -> {
                    val start = minOf(a, now)
                    val end = maxOf(maxOf(a, now), start + MIN_LOOP_MS).coerceAtMost(state.durationMs.coerceAtLeast(start + MIN_LOOP_MS))
                    player.setLoop(start, end)
                    pendingA = null
                }
            }
        },
        enabled = state.clipId != null,
    ) {
        val on = looping || pendingA != null
        Icon(Icons.Filled.Loop, contentDescription = null, tint = if (on) active else inactive, modifier = Modifier.size(18.dp))
        Text(
            when {
                looping -> " Loop"
                pendingA != null -> " A ${formatDuration(pendingA!!)}"
                else -> " A–B"
            },
            color = if (on) active else inactive,
        )
    }
}

private const val MIN_LOOP_MS = 1_000L

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
        if (state.loopEndMs >= 0 && duration > 0) {
            // The looped part of the clip.
            val loopColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(4.dp)) {
                val startX = size.width * state.loopStartMs / duration
                val endX = size.width * state.loopEndMs.coerceAtMost(duration) / duration
                drawRoundRect(
                    loopColor,
                    topLeft = Offset(startX, 0f),
                    size = Size((endX - startX).coerceAtLeast(4f), size.height),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shown), style = MaterialTheme.typography.labelMedium)
            if (state.loopEndMs >= 0) {
                Text(
                    "Looping ${formatDuration(state.loopStartMs)} – ${formatDuration(state.loopEndMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Transport(
    state: PlayerState,
    player: PlayerController,
    skipBackSec: Int,
    skipForwardSec: Int,
    onPickSkipBack: () -> Unit,
    onPickSkipForward: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        SkipButton(skipBackSec, forward = false, onClick = { player.seekBy(-skipBackSec * 1000L) }, onLongClick = onPickSkipBack)
        IconButton(onClick = player::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = player::togglePlayPause, modifier = Modifier.size(72.dp)) {
            PlayPauseIcon(state.isPlaying, Modifier.size(40.dp))
        }
        IconButton(onClick = player::next, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
        }
        SkipButton(skipForwardSec, forward = true, onClick = { player.seekBy(skipForwardSec * 1000L) }, onLongClick = onPickSkipForward)
    }
}

/** Circular-arrow button with the number of seconds inside; long-press to change the seconds. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkipButton(seconds: Int, forward: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .semantics { contentDescription = if (forward) "Skip ahead $seconds seconds" else "Rewind $seconds seconds" },
    ) {
        Icon(
            Icons.Filled.Replay,
            contentDescription = null,
            modifier = Modifier.size(40.dp).graphicsLayer { if (forward) scaleX = -1f },
        )
        Text(
            "$seconds",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
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
