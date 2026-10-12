package com.ttggg5.holovoice.data

/** How clip lists are ordered. [label] is shown in the sort menu. */
enum class ClipSort(val label: String) {
    TRACK("Track order"),
    NAME("Name"),
    TALENT("Talent"),
    DURATION("Duration"),
    RECENTLY_ADDED("Recently added"),
    MOST_PLAYED("Most played"),
    ;

    fun sorted(clips: List<ClipWithPack>): List<ClipWithPack> = when (this) {
        TRACK -> clips.sortedWith(
            compareBy<ClipWithPack, String>(NaturalOrder) { it.packName }
                .thenBy(nullsFirst(NaturalOrder)) { it.clip.category }
                .thenBy(nullsLast<Int>()) { it.clip.trackNo }
                .thenBy(NaturalOrder) { it.clip.title },
        )
        NAME -> clips.sortedWith(compareBy(NaturalOrder) { it.clip.title })
        TALENT -> clips.sortedWith(
            compareBy<ClipWithPack, String>(NaturalOrder) { it.speaker }.thenBy(NaturalOrder) { it.clip.title },
        )
        DURATION -> clips.sortedWith(compareBy(nullsLast<Long>()) { it.clip.durationMs })
        RECENTLY_ADDED -> clips.sortedByDescending { it.clip.addedAt }
        MOST_PLAYED -> clips.sortedWith(
            compareByDescending<ClipWithPack> { it.clip.playCount }.thenByDescending { it.clip.lastPlayedAt },
        )
    }
}

enum class TopicSort(val label: String) {
    NAME("Name"),
    TALENT("Talent"),
    RECENTLY_ADDED("Recently added"),
    MOST_CLIPS("Most clips"),
    ;

    fun sorted(topics: List<PackWithCount>): List<PackWithCount> = when (this) {
        NAME -> topics.sortedWith(compareBy(NaturalOrder) { it.pack.name })
        TALENT -> topics.sortedWith(
            compareBy<PackWithCount, String>(NaturalOrder) { it.pack.talent }.thenBy(NaturalOrder) { it.pack.name },
        )
        RECENTLY_ADDED -> topics.sortedByDescending { it.pack.importedAt }
        MOST_CLIPS -> topics.sortedByDescending { it.clipCount }
    }
}

enum class PlaylistSort(val label: String) {
    NAME("Name"),
    RECENT("Recently created"),
    SIZE("Most clips"),
    ;

    fun sorted(playlists: List<PlaylistWithCount>): List<PlaylistWithCount> = when (this) {
        NAME -> playlists.sortedWith(compareBy(NaturalOrder) { it.playlist.name })
        RECENT -> playlists.sortedByDescending { it.playlist.createdAt }
        SIZE -> playlists.sortedByDescending { it.entryCount }
    }
}

enum class TalentSort(val label: String) {
    NAME("Name"),
    CLIPS("Most clips"),
    PLAYS("Most played"),
    ;

    /** Unassigned (null slug) always goes last. */
    fun sorted(talents: List<TalentSummary>): List<TalentSummary> {
        val (assigned, unassigned) = talents.partition { it.slug != null }
        val sorted = when (this) {
            NAME -> assigned.sortedWith(compareBy(NaturalOrder) { it.name.orEmpty() })
            CLIPS -> assigned.sortedByDescending { it.clipCount }
            PLAYS -> assigned.sortedWith(compareByDescending<TalentSummary> { it.plays }.thenByDescending { it.clipCount })
        }
        return sorted + unassigned
    }
}

/** How clip collections are displayed. */
enum class ClipView { GRID, LIST }
