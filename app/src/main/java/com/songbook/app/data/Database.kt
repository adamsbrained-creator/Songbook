package com.songbook.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlbumEntity::class, SongEntity::class, GroupEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SongbookDatabase : RoomDatabase() {
    abstract fun dao(): SongbookDao

    companion object {
        /** 1.0 -> 1.5: images, badges, workflow stage/note, album groups. Keeps all existing data. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN image TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN badge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE songs ADD COLUMN badgeColor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN stage INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE songs ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE albums ADD COLUMN image TEXT")
                db.execSQL("ALTER TABLE albums ADD COLUMN badge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE albums ADD COLUMN badgeColor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE albums ADD COLUMN groupId INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_groups` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`gradient` INTEGER NOT NULL, " +
                        "`image` TEXT, " +
                        "`badge` TEXT NOT NULL, " +
                        "`badgeColor` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** 1.5 -> 2.0: ratings, custom types, songwriting desk, capo-adjusted chords. Keeps all existing data. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN rating REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN typeName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE songs ADD COLUMN idea TEXT NOT NULL DEFAULT ''")
                // existing songs keep showing their chords exactly as typed
                db.execSQL("ALTER TABLE songs ADD COLUMN capoAdjust INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE albums ADD COLUMN rating REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE albums ADD COLUMN typeName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE album_groups ADD COLUMN rating REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE album_groups ADD COLUMN typeName TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): SongbookDatabase =
            Room.databaseBuilder(context.applicationContext, SongbookDatabase::class.java, "songbook.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
