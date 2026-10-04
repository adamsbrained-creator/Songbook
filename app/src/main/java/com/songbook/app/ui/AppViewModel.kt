package com.songbook.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.songbook.app.SongbookApp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
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

    fun saveSong(song: SongEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.saveSong(song); onDone() }
    }

    /** Changes that should not bump "Recently Edited": badge, stage, note, artwork... */
    fun updateSongQuietly(song: SongEntity) {
        viewModelScope.launch { repo.saveSongQuietly(song) }
    }

    fun deleteSong(id: Long) {
        viewModelScope.launch { repo.deleteSong(id) }
    }

    fun duplicateSong(song: SongEntity) {
        viewModelScope.launch { repo.duplicate(song) }
    }

    fun toggleFavorite(song: SongEntity) = updateSongQuietly(song.copy(favorite = !song.favorite))

    fun moveToAlbum(song: SongEntity, albumId: Long?) = updateSongQuietly(song.copy(albumId = albumId))

    fun setArtwork(song: SongEntity, gradient: Int, image: String?) =
        updateSongQuietly(song.copy(gradient = gradient, image = image))

    fun setBadge(song: SongEntity, text: String, color: Int) =
        updateSongQuietly(song.copy(badge = text, badgeColor = color))

    fun setStage(song: SongEntity, stage: Int) = updateSongQuietly(song.copy(stage = stage))

    fun setCapo(song: SongEntity, capo: Int) = updateSongQuietly(song.copy(capo = capo))

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

    // ---- settings -------------------------------------------------------------------------
    fun setTheme(v: ThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setAccent(v: Int) = viewModelScope.launch { repo.setAccent(v) }
    fun setNavRaised(v: Boolean) = viewModelScope.launch { repo.setNavRaised(v) }
    fun setReading(l: Int, c: Int, s: Float, offset: Int) = viewModelScope.launch { repo.setReading(l, c, s, offset) }
    fun setShowChords(v: Boolean) = viewModelScope.launch { repo.setShowChords(v) }
    fun setShowTabs(v: Boolean) = viewModelScope.launch { repo.setShowTabs(v) }
    fun setAutoScroll(v: Boolean) = viewModelScope.launch { repo.setAutoScroll(v) }

    /** Everything else: `vm.edit { setScrollSpeed(5) }`. */
    fun edit(block: suspend SettingsStore.() -> Unit) = viewModelScope.launch { repo.editSettings(block) }

    // ---- data -----------------------------------------------------------------------------
    suspend fun backupJson(): String = repo.backupJson().also { repo.setLastBackup(System.currentTimeMillis()) }
    suspend fun exportText(): String = repo.exportText()
    suspend fun importData(text: String, fileName: String?): Int = repo.importData(text, fileName)
}
