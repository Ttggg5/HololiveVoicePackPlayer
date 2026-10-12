package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState

/** Compact player bar showing the clip, its talent and its topic; tap it to open Now Playing. */
@Composable
fun MiniPlayer(state: PlayerState, player: PlayerController, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val position by player.position.collectAsStateWithLifecycle(initialValue = 0L)
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .playerSwipes(state, player, onOpen)
                    .clickable(onClick = onOpen)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                // Track info slides up when the clip changes.
                AnimatedContent(
                    targetState = state,
                    contentKey = { it.clipId },
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) using
                            SizeTransform(clip = false)
                    },
                    label = "miniPlayerTrack",
                    modifier = Modifier.weight(1f),
                ) { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            if (s.talentImages.size > 1) {
                                TalentAvatars(s.talentImages, size = 40.dp, max = 2)
                            } else {
                                Artwork(
                                    s.talentImageUrl ?: s.artworkUri,
                                    Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    iconSize = 22.dp,
                                )
                            }
                            EqualizerBars(
                                playing = s.isPlaying,
                                size = 14.dp,
                                modifier = Modifier.align(Alignment.BottomEnd),
                            )
                        }
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (s.subtitle.isNotEmpty() && s.subtitle != s.title) {
                                Text(
                                    s.subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TopicLine(s.album, s.category)
                        }
                    }
                }
                IconButton(onClick = player::previous) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = player::togglePlayPause) {
                    PlayPauseIcon(state.isPlaying)
                }
                IconButton(onClick = player::next, enabled = state.hasNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}

/**
 * Mini player gestures: swipe left/right for next/previous, swipe up to open Now Playing.
 * The bar follows the finger and springs back.
 */
@Composable
private fun Modifier.playerSwipes(state: PlayerState, player: PlayerController, onOpen: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val latestState by rememberUpdatedState(state)
    val latestOpen by rememberUpdatedState(onOpen)
    val upThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    return this
        .pointerInput(player) {
            val width = size.width.toFloat()
            fun settle() {
                scope.launch {
                    val x = offsetX.value
                    val y = offsetY.value
                    when {
                        y < -upThreshold -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOpen()
                        }
                        x < -width * 0.25f && latestState.hasNext -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetX.animateTo(-width, tween(140))
                            player.next()
                            offsetX.snapTo(width * 0.4f)
                        }
                        x > width * 0.25f -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetX.animateTo(width, tween(140))
                            player.previous()
                            offsetX.snapTo(-width * 0.4f)
                        }
                    }
                    launch { offsetY.animateTo(0f, spring()) }
                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            detectDragGestures(onDragEnd = ::settle, onDragCancel = ::settle) { change, drag ->
                change.consume()
                scope.launch {
                    offsetX.snapTo(offsetX.value + drag.x)
                    offsetY.snapTo((offsetY.value + drag.y).coerceIn(-upThreshold * 2, 0f))
                }
            }
        }
        .graphicsLayer {
            translationX = offsetX.value
            translationY = offsetY.value * 0.5f
            alpha = 1f - (abs(offsetX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.6f)
        }
}

/** "📁 Topic › Category" in one muted line. */
@Composable
fun TopicLine(topic: String, category: String?, modifier: Modifier = Modifier) {
    if (topic.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp),
        )
        Text(
            listOfNotNull(topic, category).joinToString(" › "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Play/pause icon that morphs with a scale + fade. */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
        label = "playPause",
    ) { playing ->
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            modifier = modifier,
        )
    }
}
