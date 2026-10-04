package com.songbook.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongbookDao {
    @Query("SELECT * FROM songs ORDER BY updatedAt DESC")
    fun songs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM albums ORDER BY createdAt ASC")
    fun albums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun song(id: Long): SongEntity?

    @Query("SELECT * FROM albums")
    suspend fun allAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM songs")
    suspend fun allSongs(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun albumCount(): Int

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun songCount(): Int

    @Insert
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: Long)

    @Insert
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteAlbum(id: Long)
}
