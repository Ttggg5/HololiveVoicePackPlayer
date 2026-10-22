package com.ttggg5.holovoice.update

import org.json.JSONObject

/** A build published on the GitHub Releases page (tag `v0.1.N`, with the APK attached). */
data class Release(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val sizeBytes: Long,
)

object UpdateInfo {
    private val TAG = Regex("""^v?(\d+(?:\.\d+)*)$""")

    /** The release in a GitHub "latest release" response, or null when it isn't one of ours. */
    fun parseRelease(json: String): Release? = runCatching {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val name = TAG.matchEntire(o.getString("tag_name").trim())?.groupValues?.get(1) ?: return null
        // The build number (the last part) is the version code, as set by CI.
        val code = name.substringAfterLast('.').toIntOrNull() ?: return null
        val assets = o.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length()).map(assets::getJSONObject)
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return null
        val url = apk.optString("browser_download_url").takeIf { it.startsWith("https://") } ?: return null
        Release(
            versionCode = code,
            versionName = name,
            apkUrl = url,
            notes = o.optString("body").trim(),
            sizeBytes = apk.optLong("size"),
        )
    }.getOrNull()

    fun isNewer(release: Release, installedVersionCode: Long): Boolean = release.versionCode > installedVersionCode
}
