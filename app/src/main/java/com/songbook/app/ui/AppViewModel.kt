package com.songbook.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.songbook.app.SongbookApp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.BackupFile
import com.songbook.app.data.GroupEntity
import com.songbook.app.data.SettingsStore
import com.songbook.app.data.SongEntity
import com.songbook.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What can be put in your own order by dragging. */
enum class ArrangeKind { SONGS, TRACKS, ALBUMS, GROUPS }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SongbookApp).repo

    val songs: StateFlow<List<SongEntity>> =
        repo.songs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val albums: StateFlow<List<AlbumEntity>> =
        repo.albums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val groups: StateFlow<List<GroupEntity>> =
        repo.groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** False until the database has delivered its first lists (avoids treating "not loaded yet" as "deleted"). */
    val loaded: StateFlow<Boolean> =
        combine(repo.songs, repo.albums, repo.groups) { _, _, _ -> true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** False until DataStore has delivered the saved settings (avoids flashing the intro on every launch). */
    val settingsLoaded: StateFlow<Boolean> =
        repo.settings.map { true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    suspend fun getSong(id: Long): SongEntity? = repo.song(id)

    // ---- songs ----------------------------------------------------------------------------
    fun createSong(song: SongEntity, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repo.createSong(song)) }
    }

    /** Saves a whole song (editor, desk). Only pass songs that came straight from the database. */
    fun saveSong(song: SongEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.saveSong(song); onDone() }
    }

    /**
     * Changes one song. The song is read from the database first, so a copy that was only prepared for
     * drawing (with its album's cover) is never written back. [bump] counts as an edit ("Recently edited").
     */
    fun patchSong(id: Long, bump: Boolean = false, change: (SongEntity) -> SongEntity) {
        viewModelScope.launch {
            val s = repo.song(id) ?: return@launch
            val n = change(s)
            if (bump) repo.saveSong(n) else repo.saveSongQuietly(n)
        }
    }

    fun deleteSong(id: Long) {
        viewModelScope.launch { repo.deleteSong(id) }
    }

    fun duplicateSong(song: SongEntity) {
        viewModelScope.launch { repo.song(song.id)?.let { repo.duplicate(it) } }
    }

    fun toggleFavorite(song: SongEntity) = patchSong(song.id) { it.copy(favorite = !it.favorite) }

    fun moveToAlbum(song: SongEntity, albumId: Long?) = patchSong(song.id) { it.copy(albumId = albumId, side = 0, trackNo = 0) }

    /** A cover chosen by hand: the song stops following its album. */
    fun setArtwork(song: SongEntity, gradient: Int, image: String?) =
        patchSong(song.id) { it.copy(gradient = gradient, image = image, coverManual = true) }

    /** Back to the album's cover. */
    fun useAlbumCover(song: SongEntity) = patchSong(song.id) { it.copy(image = null, coverManual = false) }

    fun setBadge(song: SongEntity, text: String, color: Int) = patchSong(song.id) { it.copy(badge = text, badgeColor = color) }

    fun setStage(song: SongEntity, stage: Int) = patchSong(song.id) { it.copy(stage = stage) }

    fun setCapo(song: SongEntity, capo: Int, adjust: Boolean) = patchSong(song.id) { it.copy(capo = capo, capoAdjust = adjust) }

    fun setRating(song: SongEntity, rating: Float) = patchSong(song.id) { it.copy(rating = rating) }

    fun setType(song: SongEntity, type: String) = patchSong(song.id) { it.copy(typeName = type) }

    fun setSide(song: SongEntity, side: Int) = patchSong(song.id) { it.copy(side = side, trackNo = 0) }

    /** Chords typed on a word in the reader change the song text. */
    fun setBody(songId: Long, body: String) = patchSong(songId, bump = true) { it.copy(body = body) }

    // ---- albums & groups ------------------------------------------------------------------
    fun createAlbum(name: String, gradient: Int = 0, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repo.createAlbum(name, gradient)) }
    }

    fun updateAlbum(album: AlbumEntity) {
        viewModelScope.launch { repo.updateAlbum(album) }
    }

    fun deleteAlbum(id: Long) {
        viewModelScope.launch { repo.deleteAlbum(id) }
    }

    fun setAlbumSides(album: AlbumEntity, names: List<String>) {
        viewModelScope.launch { repo.setAlbumSides(album, names) }
    }

    fun removeAlbumSide(album: AlbumEntity, index: Int) {
        viewModelScope.launch { repo.removeAlbumSide(album, index) }
    }

    fun followAlbumCover(albumId: Long) {
        viewModelScope.launch { repo.followAlbumCover(albumId) }
    }

    fun createGroup(name: String, albumIds: List<Long>) {
        viewModelScope.launch { repo.createGroup(name, albumIds) }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch { repo.updateGroup(group) }
    }

    fun setGroupMembers(groupId: Long, albumIds: List<Long>) {
        viewModelScope.launch { repo.setGroupMembers(groupId, albumIds) }
    }

    fun ungroup(groupId: Long) {
        viewModelScope.launch { repo.deleteGroup(groupId) }
    }

    /** Saves the order [ids] (first to last) that was dragged on the Arrange page. */
    fun arrange(kind: ArrangeKind, ids: List<Long>) {
        viewModelScope.launch {
            when (kind) {
                ArrangeKind.SONGS -> repo.arrangeSongs(ids)
                ArrangeKind.TRACKS -> repo.arrangeTracks(ids)
                ArrangeKind.ALBUMS -> repo.arrangeAlbums(ids)
                ArrangeKind.GROUPS -> repo.arrangeGroups(ids)
            }
        }
    }

    // ---- settings -------------------------------------------------------------------------
    fun setTheme(v: ThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setAccent(v: Int) = viewModelScope.launch { repo.setAccent(v) }
    fun setNavRaised(v: Boolean) = viewModelScope.launch { repo.setNavRaised(v) }
    fun setReading(l: Int, c: Int, s: Float, offset: Int, section: Int) = viewModelScope.launch { repo.setReading(l, c, s, offset, section) }
    fun setShowChords(v: Boolean) = viewModelScope.launch { repo.setShowChords(v) }
    fun setShowTabs(v: Boolean) = viewModelScope.launch { repo.setShowTabs(v) }
    fun setAutoScroll(v: Boolean) = viewModelScope.launch { repo.setAutoScroll(v) }

    /** Everything else: `vm.edit { setScrollSpeed(5) }`. */
    fun edit(block: suspend SettingsStore.() -> Unit) = viewModelScope.launch { repo.editSettings(block) }

    // ---- data -----------------------------------------------------------------------------
    suspend fun backupZip(): ByteArray = repo.backupZip().also { repo.setLastBackup(System.currentTimeMillis()) }
    suspend fun backupJson(): String = repo.backupJson().also { repo.setLastBackup(System.currentTimeMillis()) }
    suspend fun exportText(): String = repo.exportText()
    suspend fun importData(text: String, fileName: String?): Int = repo.importData(text, fileName)
    fun openBackup(bytes: ByteArray): BackupFile? = repo.openBackup(bytes)
    fun previewBackup(file: BackupFile) = repo.previewBackup(file)
    suspend fun restoreBackup(file: BackupFile, replace: Boolean, withSettings: Boolean) = repo.restoreBackup(file, replace, withSettings)
}
