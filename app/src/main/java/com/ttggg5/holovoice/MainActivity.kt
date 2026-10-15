package com.ttggg5.holovoice

import com.ttggg5.holovoice.ui.components.CrashReportDialog
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ttggg5.holovoice.ui.HoloVoiceRoot
import com.ttggg5.holovoice.widget.AppShortcuts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ttggg5.holovoice.ui.theme.HoloVoiceTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleIntent(intent, fresh = savedInstanceState == null)
        setContent {
            HoloVoiceTheme {
                HoloVoiceRoot(appContainer)
                // The last crash (if any), so it can be copied and reported.
                var crash by remember { mutableStateOf(CrashLog.read(this)) }
                crash?.let { report ->
                    CrashReportDialog(report, onDismiss = {
                        CrashLog.clear(this)
                        crash = null
                    })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, fresh = true)
    }

    /**
     * The status-bar alarm icon opens Alarms; the media notification and the widget open Now Playing;
     * app-icon shortcuts start playing. [fresh] is false when the activity is only being recreated.
     */
    private fun handleIntent(intent: Intent?, fresh: Boolean) {
        if (fresh) handleShortcut(intent)
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            appContainer.openPlayerRequested.value = true
            intent.removeExtra(EXTRA_OPEN_PLAYER)
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_ALARMS, false) == true) {
            appContainer.alarms.openRequested.value = true
            intent.removeExtra(EXTRA_OPEN_ALARMS)
        }
    }

    private fun handleShortcut(intent: Intent?) {
        val container = appContainer
        val player = container.player
        val repository = container.repository
        when (intent?.action) {
            AppShortcuts.ACTION_CONTINUE -> {
                if (!player.state.value.isPlaying) player.togglePlayPause()
            }
            AppShortcuts.ACTION_SHUFFLE_ALL -> container.appScope.launch {
                player.playQueue(repository.getAllClips(), shuffle = true)
            }
            AppShortcuts.ACTION_SHUFFLE_FAVORITES -> container.appScope.launch {
                player.playQueue(repository.favorites.first(), shuffle = true)
            }
            AppShortcuts.ACTION_SHUFFLE_TALENT -> {
                val slug = intent.getStringExtra(AppShortcuts.EXTRA_TALENT) ?: return
                container.appScope.launch { player.playQueue(repository.clipsByTalent(slug).first(), shuffle = true) }
            }
            else -> return
        }
        container.openPlayerRequested.value = true
        intent?.action = null
    }

    override fun onStart() {
        super.onStart()
        appContainer.player.connect()
        // Pick up files added to or removed from imported folders, without importing again.
        if (appContainer.uiPrefs.autoSyncOnStart.value) {
            appContainer.appScope.launch { appContainer.repository.syncAll(minIntervalMs = SYNC_INTERVAL_MS) }
        }
        // "Shuffle <talent>" shortcuts for the most played talents.
        if (!shortcutsRefreshed) {
            shortcutsRefreshed = true
            appContainer.appScope.launch { AppShortcuts.refreshTalents(applicationContext, appContainer.repository) }
        }
        // Store every talent's picture in the app (first start, new talents, or a retry after being offline).
        if (!picturesChecked) {
            picturesChecked = true
            appContainer.appScope.launch {
                runCatching { appContainer.repository.downloadTalentPictures() }.onFailure { picturesChecked = false }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_ALARMS = "openAlarms"
        const val EXTRA_OPEN_PLAYER = "openPlayer"

        private const val SYNC_INTERVAL_MS = 5 * 60_000L

        /** Pictures are checked once per app process. */
        private var picturesChecked = false

        /** Talent shortcuts are refreshed once per app process. */
        private var shortcutsRefreshed = false
    }

    override fun onStop() {
        appContainer.player.disconnect()
        super.onStop()
    }
}
