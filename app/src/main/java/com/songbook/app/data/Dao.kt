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

    @Query("SELECT * FROM album_groups ORDER BY createdAt ASC")
    fun groups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun song(id: Long): SongEntity?

    @Query("SELECT * FROM albums")
    suspend fun allAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM songs")
    suspend fun allSongs(): List<SongEntity>

    @Query("SELECT * FROM album_groups")
    suspend fun allGroups(): List<GroupEntity>

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

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM album_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("UPDATE albums SET groupId = :groupId WHERE id IN (:albumIds)")
    suspend fun setAlbumsGroup(albumIds: List<Long>, groupId: Long?)

    @Query("UPDATE albums SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: Long)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM album_groups")
    suspend fun deleteAllGroups()

    /** Groups that lost all their albums are removed. */
    @Query("DELETE FROM album_groups WHERE id NOT IN (SELECT groupId FROM albums WHERE groupId IS NOT NULL)")
    suspend fun pruneGroups()
}
