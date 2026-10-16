package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Floating liquid-glass pill with the main tabs: the selected one sits in an accent bubble with its
 * label, the others show just their icon. The bubble grows and shrinks as the selection moves.
 */
@Composable
fun GlassNavBar(entries: List<NavEntry>, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
    ) {
        Surface(shape = shape, color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = 8.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.liquidGlass(shape).fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            ) {
                entries.forEach { entry ->
                    Box(Modifier.weight(if (entry.selected) 2.2f else 1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        PillItem(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PillItem(entry: NavEntry) {
    val colors = MaterialTheme.colorScheme
    val bubble by animateColorAsState(if (entry.selected) colors.primary else Color.Transparent, label = "navBubble")
    val content by animateColorAsState(if (entry.selected) colors.onPrimary else colors.onSurfaceVariant, label = "navContent")
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(bubble)
            .clickable(role = Role.Tab) {
                if (!entry.selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                entry.onClick()
            }
            .semantics {
                selected = entry.selected
                contentDescription = entry.label
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(entry.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = entry.selected,
            enter = expandHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
        ) {
            Text(
                entry.label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * The same look as [GlassNavBar] for wide windows: a floating vertical glass pill on the side, with
 * [footer] (e.g. Settings) at the bottom. The selected item's label shows under its bubble.
 */
@Composable
fun GlassNavRail(entries: List<NavEntry>, footer: NavEntry?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
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
                modifier = Modifier.liquidGlass(shape).fillMaxHeight().width(80.dp).padding(vertical = 14.dp),
            ) {
                entries.forEach { RailItem(it) }
                Spacer(Modifier.weight(1f))
                footer?.let { RailItem(it) }
            }
        }
    }
}

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
