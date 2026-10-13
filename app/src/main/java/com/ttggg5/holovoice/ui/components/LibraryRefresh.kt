package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.AppContainer
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.launch

/** Re-scans every synced folder now; the result is shown as a snackbar. */
fun refreshLibrary(container: AppContainer) {
    container.appScope.launch { container.repository.syncAll(manual = true) }
}

/** Pull down on [content] to re-scan the synced folders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRefreshBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val container = LocalContext.current.appContainer
    val syncing by container.repository.syncing.collectAsStateWithLifecycle()
    PullToRefreshBox(
        isRefreshing = syncing,
        onRefresh = { refreshLibrary(container) },
        modifier = modifier,
        content = content,
    )
}

/** Top-bar refresh button; spins while the folders are being re-scanned. */
@Composable
fun RefreshAction() {
    val container = LocalContext.current.appContainer
    val syncing by container.repository.syncing.collectAsStateWithLifecycle()
    IconButton(onClick = { refreshLibrary(container) }, enabled = !syncing) {
        if (syncing) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh library")
        }
    }
}
