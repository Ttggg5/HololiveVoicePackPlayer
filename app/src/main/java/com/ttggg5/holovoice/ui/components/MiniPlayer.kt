package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineStart
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.SolidColor
import com.ttggg5.holovoice.ui.theme.Motion
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import com.ttggg5.holovoice.appContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState

/** Compact player bar showing the clip, its talent and its topic; tap it to open Now Playing. */
@Composable
fun MiniPlayer(
    state: PlayerState,
    player: PlayerController,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onSheetDrag: (Float) -> Unit = {},
    onSheetRelease: (Float) -> Unit = {},
    /** Closes the player (swipe the bar down, or ✕ while paused). */
    onDismiss: () -> Unit = {},
) {
    val position by player.position.collectAsStateWithLifecycle(initialValue = 0L)
    // How far the bar has been pulled down towards closing.
    val offsetY = remember { Animatable(0f) }
    // How far the clip has been swiped sideways towards next/previous.
    val offsetX = remember { Animatable(0f) }
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        // Gestures are read here, outside the moving layer, so the bar can follow the finger.
        modifier = modifier.fillMaxWidth().playerSwipes(state, player, onSheetDrag, onSheetRelease, offsetX, offsetY, onDismiss).graphicsLayer {
            translationY = offsetY.value
            alpha = 1f - (offsetY.value / size.height.coerceAtLeast(1f)).coerceIn(0f, 0.8f)
        },
    ) {
        Box {
        // The playing topic's thumbnail, blurred, behind the mini player.
        BlurredCover(
            model = state.coverUri ?: state.talentImageUrl,
            wash = SolidColor(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)),
            modifier = Modifier.matchParentSize(),
            blur = 3.dp,
        )
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
                    .graphicsLayer {
                        translationX = offsetX.value
                        alpha = 1f - (abs(offsetX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.6f)
                    }
                    .clickable(onClick = onOpen)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                // Track info slides up when the clip changes.
                AnimatedContent(
                    targetState = state,
                    contentKey = { it.clipId },
                    transitionSpec = {
                        (slideInVertically(Motion.enter()) { it / 2 } + fadeIn(Motion.enter(delay = 60))) togetherWith
                            (slideOutVertically(Motion.exit()) { -it / 2 } + fadeOut(Motion.exit())) using
                            SizeTransform(clip = false)
                    },
                    label = "miniPlayerTrack",
                    modifier = Modifier.weight(1f),
                ) { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            // A collage for clips with several talents, otherwise the talent's picture.
                            Artwork(
                                s.artworkUri ?: s.talentImageUrl,
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                iconSize = 22.dp,
                            )
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
                // While paused the first button closes the player (previous is still a swipe right away).
                AnimatedContent(
                    targetState = state.isPlaying,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
                    label = "miniFirstButton",
                ) { playing ->
                    if (playing) {
                        IconButton(onClick = player::previous) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close player")
                        }
                    }
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
}

/**
 * Mini player gestures: swipe left/right for next/previous, swipe up to open Now Playing.
 * The bar follows the finger and springs back.
 */
@Composable
private fun Modifier.playerSwipes(
    state: PlayerState,
    player: PlayerController,
    onSheetDrag: (Float) -> Unit,
    onSheetRelease: (Float) -> Unit,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    onDismiss: () -> Unit,
): Modifier {
    val enabled by LocalContext.current.appContainer.uiPrefs.playerSwipes.state.collectAsStateWithLifecycle()
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val latestState by rememberUpdatedState(state)
    val latestDrag by rememberUpdatedState(onSheetDrag)
    val latestRelease by rememberUpdatedState(onSheetRelease)
    val latestDismiss by rememberUpdatedState(onDismiss)
    return this
        .pointerInput(player) {
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            // Each drag locks to one direction: up moves the player sheet with the finger, down pulls
            // the bar away to close the player, left/right moves the clip for next/previous.
            var vertical: Boolean? = null
            var closing = false
            val tracker = VelocityTracker()
            fun settle() {
                when {
                    vertical == true && closing -> scope.launch {
                        val vy = tracker.calculateVelocity().y
                        if (offsetY.value > height * 0.5f || vy > 1_500f) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetY.animateTo(height * 1.2f, tween(160))
                            latestDismiss()
                            // If the bar is still here (nothing was closed), bring it back.
                            delay(600)
                        }
                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                    }
                    vertical == true -> latestRelease(tracker.calculateVelocity().y)
                }
                when (vertical) {
                    true -> Unit
                    false -> scope.launch {
                        val x = offsetX.value
                        when {
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
                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    null -> Unit
                }
                vertical = null
            }
            detectDragGestures(
                onDragStart = {
                    vertical = null
                    closing = false
                    tracker.resetTracking()
                },
                onDragEnd = ::settle,
                onDragCancel = ::settle,
            ) { change, drag ->
                change.consume()
                tracker.addPosition(change.uptimeMillis, change.position)
                if (vertical == null) {
                    vertical = abs(drag.y) > abs(drag.x)
                    closing = vertical == true && drag.y > 0f
                }
                if (vertical == true && closing) {
                    scope.launch(start = CoroutineStart.UNDISPATCHED) { offsetY.snapTo((offsetY.value + drag.y).coerceAtLeast(0f)) }
                } else if (vertical == true) {
                    latestDrag(drag.y)
                } else {
                    scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                }
            }
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
