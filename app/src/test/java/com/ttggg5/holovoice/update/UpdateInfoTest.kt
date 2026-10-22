package com.ttggg5.holovoice.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateInfoTest {
    private fun json(tag: String, assets: String, draft: Boolean = false) = """
        {"tag_name":"$tag","body":"Redesign the widget\n","draft":$draft,"prerelease":false,
         "assets":[$assets]}
    """.trimIndent()

    private val apk = """{"name":"holovoice-0.1.83.apk","size":12345,
        "browser_download_url":"https://github.com/Ttggg5/HololiveVoicePackPlayer/releases/download/v0.1.83/holovoice-0.1.83.apk"}"""

    @Test
    fun readsTheBuildAndItsApk() {
        val r = UpdateInfo.parseRelease(json("v0.1.83", """{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}, $apk"""))!!
        assertEquals(83, r.versionCode)
        assertEquals("0.1.83", r.versionName)
        assertTrue(r.apkUrl.endsWith("/holovoice-0.1.83.apk"))
        assertEquals("Redesign the widget", r.notes)
        assertEquals(12345L, r.sizeBytes)
    }

    @Test
    fun rejectsReleasesThatArentUsableBuilds() {
        assertNull(UpdateInfo.parseRelease(json("v0.1.83", """{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}""")))
        assertNull(UpdateInfo.parseRelease(json("nightly", apk)))
        assertNull(UpdateInfo.parseRelease(json("v0.1.83", apk, draft = true)))
        assertNull(UpdateInfo.parseRelease("""{"message":"Not Found"}"""))
        assertNull(UpdateInfo.parseRelease("not json"))
    }

    @Test
    fun onlyHigherBuildsAreUpdates() {
        val r = UpdateInfo.parseRelease(json("v0.1.83", apk))!!
        assertTrue(UpdateInfo.isNewer(r, 80))
        assertFalse(UpdateInfo.isNewer(r, 83))
        assertFalse(UpdateInfo.isNewer(r, 90))
    }
}
