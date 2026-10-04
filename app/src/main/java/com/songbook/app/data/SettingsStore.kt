package com.songbook.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { Light, Dark, System }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    /** 0 = red, 1 = blue, 2 = orange */
    val accent: Int = 0,
    val navRaised: Boolean = true,
    val lyricsSize: Int = 17,
    val chordSize: Int = 14,
    val lineSpacing: Float = 1.4f,
    val showChords: Boolean = true,
    val showTabs: Boolean = true,
    val autoScroll: Boolean = false,
    val lastBackup: Long = 0L,
    val userName: String = "John",
    val seeded: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val accent = intPreferencesKey("accent")
        val navRaised = booleanPreferencesKey("nav_raised")
        val lyricsSize = intPreferencesKey("lyrics_size")
        val chordSize = intPreferencesKey("chord_size")
        val lineSpacing = floatPreferencesKey("line_spacing")
        val showChords = booleanPreferencesKey("show_chords")
        val showTabs = booleanPreferencesKey("show_tabs")
        val autoScroll = booleanPreferencesKey("auto_scroll")
        val lastBackup = longPreferencesKey("last_backup")
        val userName = stringPreferencesKey("user_name")
        val seeded = booleanPreferencesKey("seeded")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            theme = runCatching { ThemeMode.valueOf(p[K.theme] ?: "") }.getOrDefault(d.theme),
            accent = p[K.accent] ?: d.accent,
            navRaised = p[K.navRaised] ?: d.navRaised,
            lyricsSize = p[K.lyricsSize] ?: d.lyricsSize,
            chordSize = p[K.chordSize] ?: d.chordSize,
            lineSpacing = p[K.lineSpacing] ?: d.lineSpacing,
            showChords = p[K.showChords] ?: d.showChords,
            showTabs = p[K.showTabs] ?: d.showTabs,
            autoScroll = p[K.autoScroll] ?: d.autoScroll,
            lastBackup = p[K.lastBackup] ?: d.lastBackup,
            userName = p[K.userName] ?: d.userName,
            seeded = p[K.seeded] ?: d.seeded
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setTheme(v: ThemeMode) = edit { it[K.theme] = v.name }
    suspend fun setAccent(v: Int) = edit { it[K.accent] = v }
    suspend fun setNavRaised(v: Boolean) = edit { it[K.navRaised] = v }
    suspend fun setReading(lyrics: Int, chord: Int, spacing: Float) = edit {
        it[K.lyricsSize] = lyrics
        it[K.chordSize] = chord
        it[K.lineSpacing] = spacing
    }
    suspend fun setShowChords(v: Boolean) = edit { it[K.showChords] = v }
    suspend fun setShowTabs(v: Boolean) = edit { it[K.showTabs] = v }
    suspend fun setAutoScroll(v: Boolean) = edit { it[K.autoScroll] = v }
    suspend fun setLastBackup(v: Long) = edit { it[K.lastBackup] = v }
    suspend fun setUserName(v: String) = edit { it[K.userName] = v }
    suspend fun setSeeded(v: Boolean) = edit { it[K.seeded] = v }
}
