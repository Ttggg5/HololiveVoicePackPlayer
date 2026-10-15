package com.ttggg5.holovoice.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.stats.ListeningStats
import com.ttggg5.holovoice.data.stats.StatsBucket
import com.ttggg5.holovoice.data.stats.StatsPeriod
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.data.talentImages
import com.ttggg5.holovoice.data.thumbnail
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.EmptyMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

/**
 * Listening stats: one headline number (listening time), a row of stat tiles, plays over time,
 * when in the day you listen, and the top talents, clips and topics for the chosen period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, player: PlayerController, onBack: () -> Unit, onOpenTalent: (String) -> Unit, onOpenTopic: (Long) -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Listening stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            ) {
                // Period filter: one row, above everything it controls.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    StatsPeriod.entries.forEach { p ->
                        FilterChip(selected = p == period, onClick = { viewModel.setPeriod(p) }, label = { Text(p.label) })
                    }
                }
                val s = stats ?: return@Column
                if (s.plays == 0) {
                    EmptyMessage(
                        "Nothing played yet",
                        if (period == StatsPeriod.ALL) "Play some clips and your stats show up here." else "No plays in this period. Try a longer one.",
                        icon = Icons.Filled.Insights,
                        fill = false,
                    )
                    HistoryNote(s)
                    return@Column
                }
                Headline(s)
                Card("Plays ${if (period == StatsPeriod.YEAR || period == StatsPeriod.ALL) "per month" else "per day"}") {
                    PlaysChart(s.buckets)
                }
                Card("When you listen", "Plays by hour of the day") { HourStrip(s.byHour) }
                if (s.topTalents.isNotEmpty()) {
                    Card("Top talents") {
                        val max = s.topTalents.first().plays.coerceAtLeast(1)
                        s.topTalents.forEach { t ->
                            RankRow(
                                picture = t.imageUrl,
                                round = true,
                                title = t.name,
                                detail = listOfNotNull("${t.plays} plays", formatListening(t.listenedMs).takeIf { t.listenedMs > 0 }).joinToString(" · "),
                                fraction = t.plays.toFloat() / max,
                                onClick = { onOpenTalent(t.slug) },
                            )
                        }
                    }
                }
                if (s.topClips.isNotEmpty()) {
                    Card("Top clips", "Tap to play") {
                        val max = s.topClips.first().plays.coerceAtLeast(1)
                        val clips = s.topClips.map { it.clip }
                        s.topClips.forEachIndexed { i, c ->
                            RankRow(
                                picture = c.clip.thumbnail,
                                badge = c.clip.talentImages.firstOrNull()?.takeIf { c.clip.coverUri != null },
                                round = false,
                                title = c.clip.clip.title,
                                detail = "${c.plays} plays · ${c.clip.speaker}",
                                fraction = c.plays.toFloat() / max,
                                onClick = { player.playFrom(clips, i) },
                            )
                        }
                    }
                }
                if (s.topTopics.isNotEmpty()) {
                    Card("Top topics") {
                        val max = s.topTopics.first().plays.coerceAtLeast(1)
                        s.topTopics.forEach { t ->
                            RankRow(
                                picture = t.coverUri,
                                round = false,
                                placeholderFolder = true,
                                title = t.name,
                                detail = listOfNotNull("${t.plays} plays", formatListening(t.listenedMs).takeIf { t.listenedMs > 0 }).joinToString(" · "),
                                fraction = t.plays.toFloat() / max,
                                onClick = { onOpenTopic(t.packId) },
                            )
                        }
                    }
                }
                HistoryNote(s)
            }
        }
    }
}

/** "2 h 5 min", "12 min", "45 s". */
fun formatListening(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> "$h h $m min"
        m > 0 -> "$m min"
        else -> "$totalSec s"
    }
}

/** Compact counts for axis ticks: 950, 1.2K, 12K. */
private fun compact(n: Int): String = when {
    n >= 10_000 -> "${n / 1000}K"
    n >= 1_000 -> "%.1fK".format(n / 1000f)
    else -> n.toString()
}

/** The one headline number (listening time) and a row of stat tiles. */
@Composable
private fun Headline(s: ListeningStats) {
    Column {
        Text("Listening time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (s.listenedMs > 0) formatListening(s.listenedMs) else "—",
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Tile("Plays", compact(s.plays), Modifier.weight(1f))
            Tile("Clips", compact(s.clips), Modifier.weight(1f))
            Tile("Talents", s.talents.toString(), Modifier.weight(1f))
            Tile("Day streak", s.streakDays.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun Card(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Single-series column chart (one hue, no legend: the card title names it). Thin columns with a
 * rounded top and a square base, a 2dp gap between them, a hairline baseline and one recessive
 * gridline at a round max. Tap a column for its value; the busiest one is labelled on its cap.
 */
@Composable
private fun PlaysChart(buckets: List<StatsBucket>) {
    if (buckets.isEmpty()) return
    val bar = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    var selected by remember(buckets) { mutableStateOf<Int?>(null) }
    val maxPlays = buckets.maxOf { it.plays }
    // A clean top for the scale: 1, 2, 5 × 10^n at or above the busiest bucket.
    val top = niceCeil(maxPlays)
    val busiest = buckets.indexOfFirst { it.plays == maxPlays }
    val shown = selected ?: busiest
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Column {
        // The selected (or busiest) column's value, as a quiet caption instead of a label on every column.
        buckets.getOrNull(shown)?.let { b ->
            Text(
                "${b.plays} plays" + (if (b.listenedMs > 0) " · ${formatListening(b.listenedMs)}" else "") +
                    " · " + if (buckets.size > 12 && buckets.first().start.dayOfMonth == 1 && b.start.dayOfMonth == 1 && buckets.all { it.start.dayOfMonth == 1 }) {
                        b.label + " " + b.start.year
                    } else if (buckets.all { it.start.dayOfMonth == 1 } && buckets.size <= 24 && buckets.size > 1 && buckets[1].start.monthValue != buckets[0].start.monthValue) {
                        b.label + " " + b.start.year
                    } else {
                        b.start.format(dateFormat)
                    },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Canvas(
                Modifier
                    .weight(1f)
                    .height(140.dp)
                    .pointerInput(buckets) {
                        detectTapGestures { pos ->
                            val slot = size.width.toFloat() / buckets.size
                            selected = (pos.x / slot).toInt().coerceIn(0, buckets.lastIndex)
                        }
                    },
            ) {
                val slot = size.width / buckets.size
                val gap = 2.dp.toPx()
                val width = (slot - gap).coerceAtMost(24.dp.toPx()).coerceAtLeast(1f)
                val radius = 4.dp.toPx().coerceAtMost(width / 2)
                // Gridline at the scale's top, and the baseline.
                drawLine(grid, Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1.dp.toPx())
                drawLine(grid, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
                buckets.forEachIndexed { i, b ->
                    if (b.plays == 0) return@forEachIndexed
                    val h = (size.height * b.plays / top).coerceAtLeast(2.dp.toPx())
                    val left = i * slot + (slot - width) / 2
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = size.height - h,
                                right = left + width,
                                bottom = size.height,
                                topLeftCornerRadius = CornerRadius(radius),
                                topRightCornerRadius = CornerRadius(radius),
                                bottomLeftCornerRadius = CornerRadius.Zero,
                                bottomRightCornerRadius = CornerRadius.Zero,
                            ),
                        )
                    }
                    drawPath(path, if (shown == i) bar else bar.copy(alpha = 0.55f))
                }
            }
            // Y-axis: just the clean top and zero.
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(140.dp).padding(start = 6.dp),
            ) {
                Text(compact(top), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // X-axis: the first, middle and last labels only.
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, end = 24.dp)) {
            val picks = listOf(0, buckets.size / 2, buckets.lastIndex).distinct()
            picks.forEachIndexed { n, i ->
                Text(
                    buckets[i].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = when (n) {
                        0 -> androidx.compose.ui.text.style.TextAlign.Start
                        picks.lastIndex -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    },
                )
            }
        }
    }
}

/** 1, 2, 5 × 10^n at or above [n] (at least 1). */
private fun niceCeil(n: Int): Int {
    if (n <= 1) return 1
    val magnitude = 10.0.pow(kotlin.math.floor(log10(n.toDouble())))
    val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= n }
    return ceil(step * magnitude).toInt()
}

/**
 * Plays by hour as one sequential strip: a single hue from light (few) to strong (many); tap an
 * hour for its count.
 */
@Composable
private fun HourStrip(byHour: List<Int>) {
    val hue = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val max = byHour.max().coerceAtLeast(1)
    var selected by remember(byHour) { mutableStateOf<Int?>(null) }
    val peak = byHour.indexOf(byHour.max())
    val shown = selected ?: peak
    Column {
        Text(
            "%02d:00–%02d:59 · %d plays".format(shown, shown, byHour[shown]) + if (selected == null) " (busiest)" else "",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(28.dp)
                .pointerInput(byHour) {
                    detectTapGestures { pos -> selected = (pos.x / (size.width / 24f)).toInt().coerceIn(0, 23) }
                },
        ) {
            val slot = size.width / 24
            val gap = 2.dp.toPx()
            for (h in 0 until 24) {
                val n = byHour[h]
                val color = if (n == 0) empty else hue.copy(alpha = 0.2f + 0.8f * n / max)
                drawRoundRect(
                    color,
                    topLeft = Offset(h * slot + gap / 2, if (h == shown) 0f else 3.dp.toPx()),
                    size = Size(slot - gap, if (h == shown) size.height else size.height - 6.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("0", "6", "12", "18", "24").forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = if (i == 4) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start,
                )
            }
        }
    }
}

/** A ranked row: picture, name, detail line, and a thin bar for its share of the leader. */
@Composable
private fun RankRow(
    picture: Any?,
    round: Boolean,
    title: String,
    detail: String,
    fraction: Float,
    onClick: () -> Unit,
    badge: String? = null,
    placeholderFolder: Boolean = false,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Box(Modifier.size(44.dp)) {
            if (placeholderFolder) {
                Artwork(picture, Modifier.size(40.dp).clip(shape), iconSize = 20.dp, placeholder = Icons.Filled.Folder)
            } else {
                Artwork(picture, Modifier.size(40.dp).clip(shape), iconSize = 20.dp)
            }
            if (badge != null) {
                Artwork(badge, Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape), iconSize = 10.dp)
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun HistoryNote(s: ListeningStats) {
    val since = s.historySince ?: return
    val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    Text(
        "Detailed history (listening time, days and hours) starts on $date. \"All time\" play counts also include earlier plays.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
