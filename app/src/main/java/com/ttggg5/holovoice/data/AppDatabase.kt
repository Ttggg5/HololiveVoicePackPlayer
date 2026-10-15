package com.ttggg5.holovoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Pack::class, Clip::class, Playlist::class, PlaylistEntry::class, ClipTalent::class, Alarm::class, PlayEvent::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun clipDao(): ClipDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun clipTalentDao(): ClipTalentDao
    abstract fun alarmDao(): AlarmDao
    abstract fun playEventDao(): PlayEventDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "holovoice.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}

/** v2 adds user playlists; existing packs and clips are kept. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`playlistId` INTEGER NOT NULL, `clipId` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`clipId`) REFERENCES `clips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_entries_playlistId` ON `playlist_entries` (`playlistId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_entries_clipId` ON `playlist_entries` (`clipId`)")
    }
}

/** v3 adds the talent (profile picture) matched to each clip. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `talentSlug` TEXT")
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `talentName` TEXT")
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `talentImageUrl` TEXT")
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `talentManual` INTEGER NOT NULL DEFAULT 0")
    }
}

/** v4 groups a picked folder into one topic; sub-folders become categories. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `category` TEXT")
    }
}

/** v5 tracks when clips were added and played, for sorting and the Home screen. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `addedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `lastPlayedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `clips` ADD COLUMN `playCount` INTEGER NOT NULL DEFAULT 0")
        // Existing clips count as added when their topic was imported.
        db.execSQL("UPDATE clips SET addedAt = (SELECT importedAt FROM packs WHERE packs.id = clips.packId)")
    }
}

/** v6 lets a clip have several talents; existing single talents are copied over. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `clip_talents` (`clipId` INTEGER NOT NULL, `slug` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `imageUrl` TEXT, `position` INTEGER NOT NULL, PRIMARY KEY(`clipId`, `slug`), " +
                "FOREIGN KEY(`clipId`) REFERENCES `clips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_clip_talents_slug` ON `clip_talents` (`slug`)")
        db.execSQL(
            "INSERT OR IGNORE INTO clip_talents (clipId, slug, name, imageUrl, position) " +
                "SELECT id, talentSlug, COALESCE(talentName, talentSlug), talentImageUrl, 0 FROM clips WHERE talentSlug IS NOT NULL",
        )
    }
}

/** v7 adds voice alarms. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `alarms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `days` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, " +
                "`label` TEXT NOT NULL, `source` TEXT NOT NULL, `clipId` INTEGER, `talentSlug` TEXT, `packId` INTEGER, " +
                "`snoozeMinutes` INTEGER NOT NULL, `snoozeUntil` INTEGER NOT NULL)",
        )
    }
}

/** v8 keeps a history of plays (when, and how long was listened) for listening stats. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`clipId` INTEGER NOT NULL, `playedAt` INTEGER NOT NULL, `listenedMs` INTEGER NOT NULL, " +
                "FOREIGN KEY(`clipId`) REFERENCES `clips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_clipId` ON `play_events` (`clipId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_playedAt` ON `play_events` (`playedAt`)")
    }
}
