package com.songbook.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.songbook.app.SongbookApp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.SongEntity
import com.songbook.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SongbookApp).repo

    val songs: StateFlow<List<SongEntity>> =
        repo.songs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    /** False until the database has delivered its first list (avoids treating "not loaded yet" as "deleted"). */
    val loaded: StateFlow<Boolean> =
        repo.songs.map { true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val albums: StateFlow<List<AlbumEntity>> =
        repo.albums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    suspend fun getSong(id: Long): SongEntity? = repo.song(id)

    // ---- songs ----------------------------------------------------------------------------
    fun createSong(song: SongEntity, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repo.createSong(song)) }
    }

    fun saveSong(song: SongEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch { repo.saveSong(song); onDone() }
    }

    fun deleteSong(id: Long) {
        viewModelScope.launch { repo.deleteSong(id) }
    }

    fun duplicateSong(song: SongEntity) {
        viewModelScope.launch { repo.duplicate(song) }
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch { repo.saveSongQuietly(song.copy(favorite = !song.favorite)) }
    }

    fun moveToAlbum(song: SongEntity, albumId: Long?) {
        viewModelScope.launch { repo.saveSongQuietly(song.copy(albumId = albumId)) }
    }

    fun setGradient(song: SongEntity, gradient: Int) {
        viewModelScope.launch { repo.saveSongQuietly(song.copy(gradient = gradient)) }
    }

    fun setCapo(song: SongEntity, capo: Int) {
        viewModelScope.launch { repo.saveSongQuietly(song.copy(capo = capo)) }
    }

    // ---- albums ---------------------------------------------------------------------------
    fun createAlbum(name: String, gradient: Int = 0, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repo.createAlbum(name, gradient)) }
    }

    fun updateAlbum(album: AlbumEntity) {
        viewModelScope.launch { repo.updateAlbum(album) }
    }

    fun deleteAlbum(id: Long) {
        viewModelScope.launch { repo.deleteAlbum(id) }
    }

    // ---- settings -------------------------------------------------------------------------
    fun setTheme(v: ThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setAccent(v: Int) = viewModelScope.launch { repo.setAccent(v) }
    fun setNavRaised(v: Boolean) = viewModelScope.launch { repo.setNavRaised(v) }
    fun setReading(l: Int, c: Int, s: Float) = viewModelScope.launch { repo.setReading(l, c, s) }
    fun setShowChords(v: Boolean) = viewModelScope.launch { repo.setShowChords(v) }
    fun setShowTabs(v: Boolean) = viewModelScope.launch { repo.setShowTabs(v) }
    fun setAutoScroll(v: Boolean) = viewModelScope.launch { repo.setAutoScroll(v) }

    // ---- data -----------------------------------------------------------------------------
    suspend fun backupJson(): String = repo.backupJson().also { repo.setLastBackup(System.currentTimeMillis()) }
    suspend fun exportText(): String = repo.exportText()
    suspend fun importData(text: String, fileName: String?): Int = repo.importData(text, fileName)
}
