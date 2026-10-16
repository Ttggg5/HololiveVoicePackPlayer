package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.lerp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One destination in [GlassNavBar] / [GlassNavRail]. */
data class NavEntry(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/**
 * Floating liquid-glass pill with the main tabs. One accent bubble slides between the tabs, following the
 * page as it is swiped or animates ([position] is the pager's fractional page, e.g. 1.4 = 40% from tab 1
 * to tab 2): the tab under it widens and shows its label, the others shrink to just their icon.
 */
@Composable
fun GlassNavBar(entries: List<NavEntry>, position: () -> Float, modifier: Modifier = Modifier) {
    val shape = CircleShape
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
    ) {
        Surface(shape = shape, color = Color.Transparent, contentColor = colors.onSurface, shadowElevation = 8.dp) {
            BoxWithConstraints(Modifier.liquidGlass(shape).fillMaxWidth().height(60.dp).padding(horizontal = 6.dp)) {
                val pos = position().coerceIn(0f, (entries.size - 1).coerceAtLeast(0).toFloat())
                // How much each tab is "selected" (1 under the bubble, fading to 0 one tab away).
                val closeness = entries.indices.map { i -> (1f - abs(i - pos)).coerceIn(0f, 1f) }
                val weights = closeness.map { 1f + EXTRA_WEIGHT * it }
                val total = weights.sum()
                val width = maxWidth
                val starts = weights.runningFold(0f) { acc, w -> acc + w }.map { width * (it / total) }
                // The bubble spans the slot at the fractional position, between its two neighbours.
                val lo = floor(pos).toInt().coerceIn(0, entries.lastIndex)
                val hi = (lo + 1).coerceAtMost(entries.lastIndex)
                val f = pos - lo
                val left = lerp(starts[lo], starts[hi], f)
                val right = lerp(starts[lo + 1], starts[hi + 1], f)
                Box(
                    Modifier
                        .offset(x = left + 2.dp)
                        .width((right - left - 4.dp).coerceAtLeast(0.dp))
                        .fillMaxHeight()
                        .padding(vertical = 7.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    entries.forEachIndexed { i, entry ->
                        val c = closeness[i]
                        val tint = lerp(colors.onSurfaceVariant, colors.onPrimary, c)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(weights[i])
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(role = Role.Tab) {
                                    if (!entry.selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    entry.onClick()
                                }
                                .semantics {
                                    selected = entry.selected
                                    contentDescription = entry.label
                                },
                        ) {
                            Icon(entry.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                            if (c > 0.01f) {
                                Text(
                                    entry.label,
                                    color = tint.copy(alpha = c),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.revealWidth(c).padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Extra width (in tab widths) of the tab under the bubble, for its label. */
private const val EXTRA_WEIGHT = 1.3f

/** Lays the content out at [fraction] of its natural width, clipped, so it grows in smoothly. */
private fun Modifier.revealWidth(fraction: Float): Modifier = clipToBounds().layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val w = (p.width * fraction).roundToInt().coerceAtMost(constraints.maxWidth)
    layout(w, p.height) { p.placeRelative(0, 0) }
}

/**
 * The same look as [GlassNavBar] for wide windows: a floating vertical glass pill on the side, with
 * [footer] (e.g. Settings) at the bottom. The bubble slides up and down with the page, like the bar's.
 */
@Composable
fun GlassNavRail(entries: List<NavEntry>, position: () -> Float, footer: NavEntry?, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
    ) {
        Surface(shape = shape, color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = 8.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.liquidGlass(shape).fillMaxHeight().width(80.dp).padding(vertical = 24.dp),
            ) {
                RailTabs(entries, position)
                Spacer(Modifier.weight(1f))
                footer?.let { RailItem(it) }
            }
        }
    }
}

@Composable
private fun RailTabs(entries: List<NavEntry>, position: () -> Float) {
    val colors = MaterialTheme.colorScheme
    val pos = position().coerceIn(0f, (entries.size - 1).coerceAtLeast(0).toFloat())
    Box(Modifier.width(80.dp).height(RAIL_SLOT * entries.size)) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = RAIL_SLOT * pos + 4.dp)
                .size(width = 56.dp, height = 36.dp)
                .clip(CircleShape)
                .background(colors.primary),
        )
        Column {
            entries.forEachIndexed { i, entry ->
                val c = (1f - abs(i - pos)).coerceIn(0f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(80.dp)
                        .height(RAIL_SLOT)
                        .clickable(role = Role.Tab, onClick = entry.onClick)
                        .semantics {
                            selected = entry.selected
                            contentDescription = entry.label
                        }
                        .padding(top = 11.dp),
                ) {
                    Icon(entry.icon, contentDescription = null, tint = lerp(colors.onSurfaceVariant, colors.onPrimary, c), modifier = Modifier.size(22.dp))
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary.copy(alpha = c),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                }
            }
        }
    }
}

/** Height of one tab in [GlassNavRail]. */
private val RAIL_SLOT = 68.dp

@Composable
private fun RailItem(entry: NavEntry) {
    val colors = MaterialTheme.colorScheme
    val bubble by animateColorAsState(if (entry.selected) colors.primary else Color.Transparent, label = "railBubble")
    val content by animateColorAsState(if (entry.selected) colors.onPrimary else colors.onSurfaceVariant, label = "railContent")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Tab, onClick = entry.onClick)
            .semantics {
                selected = entry.selected
                contentDescription = entry.label
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier.clip(CircleShape).background(bubble).padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        AnimatedVisibility(visible = entry.selected, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(
                entry.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
