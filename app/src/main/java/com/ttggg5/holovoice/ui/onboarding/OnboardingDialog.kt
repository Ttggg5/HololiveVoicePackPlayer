package com.ttggg5.holovoice.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: String, val body: String)

private val PAGES = listOf(
    Page(
        Icons.Filled.CreateNewFolder,
        "Import your voice packs",
        "Use \"Import many topics\" and pick the folder that holds all your packs: each sub-folder becomes a topic. " +
            "New files and folders are picked up automatically every time you open the app.",
    ),
    Page(
        Icons.Filled.Face,
        "Name files after the talent",
        "Call a file 兎田ぺこら.mp3 or Usada Pekora.mp3 and the app shows that talent's picture and " +
            "groups it under their name in the Talents tab.",
    ),
    Page(
        Icons.Filled.TouchApp,
        "Tap to play, long-press for more",
        "Tap a clip to play from there. Long-press for Play next, Add to queue, playlists and Change talent. " +
            "Use the sort and grid/list buttons to arrange any list.",
    ),
)

/** Three-page intro shown on first launch (and from Home → How it works). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(onDone: () -> Unit) {
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDone) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(300.dp)) { index ->
                    val page = PAGES[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Box(
                            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
                        }
                        Text(page.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
                        Text(
                            page.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    repeat(PAGES.size) { i ->
                        val active = pager.currentPage == i
                        val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "dotWidth")
                        val color by animateColorAsState(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            label = "dotColor",
                        )
                        Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    TextButton(onClick = onDone) { Text("Skip") }
                    val last = pager.currentPage == PAGES.lastIndex
                    Button(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                        Text(if (last) "Get started" else "Next")
                    }
                }
            }
        }
    }
}
