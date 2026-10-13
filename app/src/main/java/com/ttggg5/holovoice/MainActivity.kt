package com.ttggg5.holovoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ttggg5.holovoice.ui.HoloVoiceRoot
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
        setContent {
            HoloVoiceTheme {
                HoloVoiceRoot(appContainer)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.player.connect()
        // Pick up files added to or removed from imported folders, without importing again.
        if (appContainer.uiPrefs.autoSyncOnStart.value) {
            appContainer.appScope.launch { appContainer.repository.syncAll(minIntervalMs = SYNC_INTERVAL_MS) }
        }
        // Store every talent's picture in the app (first start, new talents, or a retry after being offline).
        if (!picturesChecked) {
            picturesChecked = true
            appContainer.appScope.launch {
                runCatching { appContainer.repository.downloadTalentPictures() }.onFailure { picturesChecked = false }
            }
        }
    }

    private companion object {
        const val SYNC_INTERVAL_MS = 5 * 60_000L

        /** Pictures are checked once per app process. */
        var picturesChecked = false
    }

    override fun onStop() {
        appContainer.player.disconnect()
        super.onStop()
    }
}
