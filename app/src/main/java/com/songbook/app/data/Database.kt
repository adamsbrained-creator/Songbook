package com.songbook.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlbumEntity::class, SongEntity::class], version = 1, exportSchema = false)
abstract class SongbookDatabase : RoomDatabase() {
    abstract fun dao(): SongbookDao

    companion object {
        fun build(context: Context): SongbookDatabase =
            Room.databaseBuilder(context.applicationContext, SongbookDatabase::class.java, "songbook.db").build()
    }
}
