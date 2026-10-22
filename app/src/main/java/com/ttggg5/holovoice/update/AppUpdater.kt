package com.ttggg5.holovoice.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Where the in-app updater is. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val checkedAt: Long) : UpdateState
    data class Available(val release: Release) : UpdateState
    /** Android needs "Install unknown apps" allowed for this app first. */
    data class NeedsPermission(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    /** Handed to Android, which asks the user to confirm the update. */
    data class Installing(val release: Release) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}

/**
 * Updates the app from the GitHub Releases page, where CI publishes every build signed with the
 * app's key: checks for a newer build (quietly once a day, or when asked), downloads it into an
 * install session and lets Android ask the user to confirm.
 */
class AppUpdater(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("update", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** The version code of the update the user chose "Later" for (the Home card stays hidden for it). */
    private val _dismissed = MutableStateFlow(prefs.getInt("dismissed", 0))
    val dismissed: StateFlow<Int> = _dismissed.asStateFlow()

    val installedVersionCode: Long
        get() = runCatching {
            val info = app.packageManager.getPackageInfo(app.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }.getOrDefault(0L)

    val installedVersionName: String
        get() = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull().orEmpty()

    val lastCheckedAt: Long get() = prefs.getLong("checkedAt", 0L)

    /** A quiet check when the last one was more than a day ago. */
    suspend fun checkDaily() {
        if (System.currentTimeMillis() - lastCheckedAt > DAY_MS) check(quiet = true)
    }

    /** Looks for a newer build. A [quiet] check keeps errors to itself. */
    suspend fun check(quiet: Boolean) {
        val busy = _state.value
        if (busy is UpdateState.Downloading || busy is UpdateState.Installing || busy is UpdateState.Checking) return
        if (!quiet) _state.value = UpdateState.Checking
        try {
            val release = withContext(Dispatchers.IO) { UpdateInfo.parseRelease(fetch(LATEST_URL)) }
            val now = System.currentTimeMillis()
            prefs.edit().putLong("checkedAt", now).apply()
            _state.value = if (release != null && UpdateInfo.isNewer(release, installedVersionCode)) {
                UpdateState.Available(release)
            } else {
                UpdateState.UpToDate(now)
            }
        } catch (e: Exception) {
            _state.value = if (quiet) UpdateState.Idle else UpdateState.Failed("Couldn't check for updates. Check your connection.")
        }
    }

    fun dismiss(release: Release) {
        prefs.edit().putInt("dismissed", release.versionCode).apply()
        _dismissed.value = release.versionCode
    }

    /** True when Android lets this app install updates (the "Install unknown apps" switch). */
    fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    /** Opens the system page where "Install unknown apps" is allowed for this app. */
    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Downloads [release] into an install session and hands it to Android to confirm. */
    suspend fun downloadAndInstall(release: Release) {
        if (!canInstall()) {
            _state.value = UpdateState.NeedsPermission(release)
            return
        }
        _state.value = UpdateState.Downloading(release, 0f)
        val installer = app.packageManager.packageInstaller
        var sessionId = -1
        try {
            sessionId = withContext(Dispatchers.IO) {
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(app.packageName)
                }
                val id = installer.createSession(params)
                installer.openSession(id).use { session ->
                    val conn = open(release.apkUrl, accept = "application/octet-stream")
                    try {
                        val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                        session.openWrite("holovoice.apk", 0, total.takeIf { it > 0 } ?: -1).use { out ->
                            conn.inputStream.use { input ->
                                val buffer = ByteArray(64 * 1024)
                                var done = 0L
                                var lastShown = 0f
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    out.write(buffer, 0, n)
                                    done += n
                                    if (total > 0) {
                                        val p = (done.toFloat() / total).coerceIn(0f, 1f)
                                        if (p - lastShown >= 0.01f) {
                                            lastShown = p
                                            _state.value = UpdateState.Downloading(release, p)
                                        }
                                    }
                                }
                            }
                            session.fsync(out)
                        }
                    } finally {
                        conn.disconnect()
                    }
                    _state.value = UpdateState.Installing(release)
                    val callback = PendingIntent.getBroadcast(
                        app,
                        id,
                        Intent(app, InstallResultReceiver::class.java).putExtra(EXTRA_VERSION, release.versionCode),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    session.commit(callback.intentSender)
                }
                id
            }
        } catch (e: Exception) {
            if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
            _state.value = UpdateState.Failed("Couldn't download the update. Check your connection and try again.", release)
        }
    }

    /** Called by [InstallResultReceiver] when Android reports how the install went. */
    internal fun onInstallFailed(message: String) {
        val release = (_state.value as? UpdateState.Installing)?.release
        _state.value = UpdateState.Failed(message, release)
    }

    internal fun onInstallCancelled() {
        val release = (_state.value as? UpdateState.Installing)?.release
        _state.value = release?.let { UpdateState.Available(it) } ?: UpdateState.Idle
    }

    private fun open(url: String, accept: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        // GitHub sends release downloads to its storage host (https → https, followed automatically).
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "HoloVoice/$installedVersionName")
        conn.setRequestProperty("Accept", accept)
        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            conn.disconnect()
            error("HTTP $code")
        }
        return conn
    }

    private fun fetch(url: String): String {
        val conn = open(url, accept = "application/vnd.github+json")
        try {
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val REPO_URL = "https://github.com/Ttggg5/HololiveVoicePackPlayer"
        private const val LATEST_URL = "https://api.github.com/repos/Ttggg5/HololiveVoicePackPlayer/releases/latest"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        internal const val EXTRA_VERSION = "version"
    }
}

/** Receives the install session's result: shows Android's confirmation, or reports a failure. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updater = context.appContainer.updater
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // On success the app is replaced and restarted; nothing to do here.
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> updater.onInstallCancelled()
            else -> {
                val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                updater.onInstallFailed(
                    when (status) {
                        PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                            "Android refused the update: it's signed with a different key than the installed app. Uninstall once and install the new version."
                        PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage to install the update."
                        else -> "The update couldn't be installed" + (detail?.let { ": $it" } ?: ".")
                    },
                )
            }
        }
    }
}
