package com.ttggg5.holovoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Pack::class, Clip::class, Playlist::class, PlaylistEntry::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun clipDao(): ClipDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "holovoice.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
