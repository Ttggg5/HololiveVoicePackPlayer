package com.ttggg5.holovoice.data.talents

import com.ttggg5.holovoice.data.AppDatabase
import com.ttggg5.holovoice.data.Clip
import com.ttggg5.holovoice.data.ClipTalent

/** Assigns talents (and their profile pictures) to clips, automatically or by the user's choice. */
class TalentLinker(private val db: AppDatabase, val directory: TalentDirectory) {

    /**
     * Finds every talent whose name appears in each clip's name (its file name without track number).
     * Clips the user assigned by hand are left alone. Returns how many clips got at least one talent.
     */
    suspend fun matchClips(clips: List<Clip>, talents: List<Talent>): Int {
        if (talents.isEmpty()) return 0
        var matched = 0
        for (clip in clips) {
            if (clip.talentManual) {
                if (clip.talentSlug != null) matched++
                continue
            }
            val found = TalentMatcher.matchAll(clip.title, talents)
            if (found.isNotEmpty()) matched++
            db.clipTalentDao().replace(clip.id, found.map { it.toClipTalent(clip.id) }, manual = false)
        }
        return matched
    }

    /** The user's pick for [clipIds]; an empty list clears their talents. */
    suspend fun setTalents(clipIds: List<Long>, talents: List<Talent>) {
        for (id in clipIds) {
            db.clipTalentDao().replace(id, talents.map { it.toClipTalent(id) }, manual = talents.isNotEmpty())
        }
    }

    private fun Talent.toClipTalent(clipId: Long) = ClipTalent(clipId, slug, nameEn, imageUrl, 0)
}
