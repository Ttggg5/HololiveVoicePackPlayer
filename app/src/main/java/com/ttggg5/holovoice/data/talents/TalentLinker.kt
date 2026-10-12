package com.ttggg5.holovoice.data.talents

import com.ttggg5.holovoice.data.AppDatabase
import com.ttggg5.holovoice.data.Clip

/** Assigns talents (and their profile pictures) to clips, automatically or by the user's choice. */
class TalentLinker(private val db: AppDatabase, val directory: TalentDirectory) {

    /**
     * Matches each clip's name (its file name without track number) against the talent list.
     * Clips the user assigned by hand are left alone. Returns how many clips got a talent.
     */
    suspend fun matchClips(clips: List<Clip>, talents: List<Talent>): Int {
        if (talents.isEmpty()) return 0
        var matched = 0
        for (clip in clips) {
            if (clip.talentManual) {
                if (clip.talentSlug != null) matched++
                continue
            }
            val talent = TalentMatcher.match(clip.title, talents)
            if (talent != null) matched++
            if (talent?.slug != clip.talentSlug || talent?.imageUrl != clip.talentImageUrl) {
                db.clipDao().setTalent(listOf(clip.id), talent?.slug, talent?.nameEn, talent?.imageUrl, manual = false)
            }
        }
        return matched
    }

    /** The user's pick for [clipIds]; null clears the talent. */
    suspend fun setTalent(clipIds: List<Long>, talent: Talent?) {
        clipIds.chunked(900).forEach { ids ->
            db.clipDao().setTalent(ids, talent?.slug, talent?.nameEn, talent?.imageUrl, manual = talent != null)
        }
    }
}
