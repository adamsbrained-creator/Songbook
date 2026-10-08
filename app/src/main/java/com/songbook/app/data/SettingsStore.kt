package com.songbook.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
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

/** Song page theme: follow the app, or force dark / light. */
object SongTheme {
    const val APP = 0
    const val DARK = 1
    const val LIGHT = 2
}

/** How "# Verse 1" headings are shown on the song page. */
object SectionStyle {
    const val UPPER = 0
    const val LOWER = 1
    const val AS_WRITTEN = 2
}

/** The three looks of the app. */
object UiStyle {
    const val OLD = 0
    const val NEW = 1
    const val GLASS = 2
    val names = listOf("Old", "New", "Glass")
}

/** What the big song card shows in the "New" look. */
object CardFace {
    const val ARTWORK = 0
    const val LYRICS = 1
    const val CHORDS = 2
}

/** Layout of the song list: rows, small rows, or tiles in 2, 3 or 4 columns. */
object SongsLayout {
    const val LIST = 0
    const val COMPACT = 1
    const val GRID2 = 2
    const val GRID3 = 3
    const val GRID4 = 4
    val names = listOf("List", "Compact list", "Grid, 2 columns", "Grid, 3 columns", "Grid, 4 columns")

    /** Columns for the tile layouts, 0 for the two list layouts. */
    fun columns(v: Int) = if (v >= GRID2) v else 0
}

/** Columns for albums and groups: 1 = wide rows. */
val ColumnChoices = listOf("1 column (rows)", "2 columns", "3 columns", "4 columns")

data class AppSettings(
    val uiStyle: Int = UiStyle.OLD,
    val theme: ThemeMode = ThemeMode.System,
    /** 0 = red, 1 = blue, 2 = orange, 3 = your own color ([accentCustom]) */
    val accent: Int = 0,
    /** ARGB of the custom accent color chosen with the RGB sliders. */
    val accentCustom: Int = 0xFF8A63D2.toInt(),
    /** How many albums are shown at the top of the main page. 0 hides them. */
    val topAlbums: Int = 4,
    val cardFace: Int = CardFace.ARTWORK,
    /** Last opened song, shown in the bottom pill of the "New" look. */
    val lastSongId: Long = 0L,
    val navRaised: Boolean = true,

    // reading
    val lyricsSize: Int = 17,
    val chordSize: Int = 14,
    val lineSpacing: Float = 1.4f,
    /** Extra space (dp, can be negative) between a chord and the lyric below it. */
    val chordOffset: Int = 0,
    val sectionStyle: Int = SectionStyle.UPPER,
    /** Size of "VERSE ONE" style headings in pt. */
    val sectionSize: Int = 12,

    // song display
    val showChords: Boolean = true,
    val showTabs: Boolean = true,
    val autoScroll: Boolean = false,
    /** 1..10 */
    val scrollSpeed: Int = 3,
    val showEditButton: Boolean = true,
    val songTheme: Int = SongTheme.APP,
    /** false = "Chords" page, true = plain "Lyrics" page on the song screen. */
    val lyricsView: Boolean = false,

    // layouts (see [SongsLayout], [ColumnChoices])
    val songsLayout: Int = SongsLayout.LIST,
    /** 1..4 columns for the albums on the Albums page and inside a group. */
    val albumColumns: Int = 2,
    /** 1..4 columns for the groups on the Albums page. */
    val groupColumns: Int = 2,
    /** 0 = list, 2 / 3 = tiles, for the songs inside an album. */
    val trackLayout: Int = 0,
    /** 0 = list, 2 / 3 = tiles, for the Favorites page. */
    val favLayout: Int = 0,

    // sorting (see Sorting.kt)
    val songSort: Int = SongSort.RECENT,
    val albumSort: Int = AlbumSort.CREATED,
    val trackSort: Int = TrackSort.ALBUM,

    /** Replaces the "Groups" heading on the Albums page. Blank = named after the type you chose ("Trilogies"). */
    val groupsTitle: String = "",
    /** Order and visibility of the blocks on the main page: "filters:1,recent:0,...". Blank = default. */
    val homeLayout: String = "",
    /** Same for the Albums page: "groups:1,albums:1". */
    val albumsLayout: String = "",

    // profile
    val userName: String = "",
    /** 0 Hello, 1 Hi, 2 Hey, 3 Welcome back, 4 by time of day, 5 custom */
    val greeting: Int = 0,
    val greetingCustom: String = "",
    val onboarded: Boolean = false,

    val lastBackup: Long = 0L,
    val seeded: Boolean = false,
    /** Old songs that had the same picture as their album were switched to follow the album once. */
    val coversAdopted: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object K {
        val uiStyle = intPreferencesKey("ui_style")
        val accentCustom = intPreferencesKey("accent_custom")
        val topAlbums = intPreferencesKey("top_albums")
        val cardFace = intPreferencesKey("card_face")
        val lastSongId = longPreferencesKey("last_song_id")
        val theme = stringPreferencesKey("theme")
        val accent = intPreferencesKey("accent")
        val navRaised = booleanPreferencesKey("nav_raised")
        val lyricsSize = intPreferencesKey("lyrics_size")
        val chordSize = intPreferencesKey("chord_size")
        val lineSpacing = floatPreferencesKey("line_spacing")
        val chordOffset = intPreferencesKey("chord_offset")
        val sectionStyle = intPreferencesKey("section_style")
        val sectionSize = intPreferencesKey("section_size")
        val showChords = booleanPreferencesKey("show_chords")
        val showTabs = booleanPreferencesKey("show_tabs")
        val autoScroll = booleanPreferencesKey("auto_scroll")
        val scrollSpeed = intPreferencesKey("scroll_speed")
        val showEditButton = booleanPreferencesKey("show_edit_button")
        val songTheme = intPreferencesKey("song_theme")
        val lyricsView = booleanPreferencesKey("lyrics_view")
        val gridColumns = intPreferencesKey("grid_columns")
        val songsGrid = booleanPreferencesKey("songs_grid")
        val songsLayout = intPreferencesKey("songs_layout")
        val albumColumns = intPreferencesKey("album_columns")
        val groupColumns = intPreferencesKey("group_columns")
        val trackLayout = intPreferencesKey("track_layout")
        val favLayout = intPreferencesKey("fav_layout")
        val songSort = intPreferencesKey("song_sort")
        val albumSort = intPreferencesKey("album_sort")
        val trackSort = intPreferencesKey("track_sort")
        val groupsTitle = stringPreferencesKey("groups_title")
        val homeLayout = stringPreferencesKey("home_layout")
        val albumsLayout = stringPreferencesKey("albums_layout")
        val userName = stringPreferencesKey("user_name")
        val greeting = intPreferencesKey("greeting")
        val greetingCustom = stringPreferencesKey("greeting_custom")
        val onboarded = booleanPreferencesKey("onboarded")
        val lastBackup = longPreferencesKey("last_backup")
        val seeded = booleanPreferencesKey("seeded")
        val coversAdopted = booleanPreferencesKey("covers_adopted")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        // 2.0 had one "columns" and one "songs as grid" switch; they become the new per-page choices
        val oldColumns = (p[K.gridColumns] ?: 2).coerceIn(2, 3)
        val oldGrid = p[K.songsGrid] ?: false
        AppSettings(
            uiStyle = (p[K.uiStyle] ?: d.uiStyle).coerceIn(0, 2),
            accentCustom = p[K.accentCustom] ?: d.accentCustom,
            topAlbums = (p[K.topAlbums] ?: d.topAlbums).coerceIn(0, 8),
            cardFace = (p[K.cardFace] ?: d.cardFace).coerceIn(0, 2),
            lastSongId = p[K.lastSongId] ?: d.lastSongId,
            theme = runCatching { ThemeMode.valueOf(p[K.theme] ?: "") }.getOrDefault(d.theme),
            accent = p[K.accent] ?: d.accent,
            navRaised = p[K.navRaised] ?: d.navRaised,
            lyricsSize = p[K.lyricsSize] ?: d.lyricsSize,
            chordSize = p[K.chordSize] ?: d.chordSize,
            lineSpacing = p[K.lineSpacing] ?: d.lineSpacing,
            chordOffset = p[K.chordOffset] ?: d.chordOffset,
            sectionStyle = p[K.sectionStyle] ?: d.sectionStyle,
            sectionSize = (p[K.sectionSize] ?: d.sectionSize).coerceIn(9, 30),
            showChords = p[K.showChords] ?: d.showChords,
            showTabs = p[K.showTabs] ?: d.showTabs,
            autoScroll = p[K.autoScroll] ?: d.autoScroll,
            scrollSpeed = p[K.scrollSpeed] ?: d.scrollSpeed,
            showEditButton = p[K.showEditButton] ?: d.showEditButton,
            songTheme = p[K.songTheme] ?: d.songTheme,
            lyricsView = p[K.lyricsView] ?: d.lyricsView,
            songsLayout = (p[K.songsLayout] ?: if (oldGrid) oldColumns else SongsLayout.LIST).coerceIn(0, 4),
            albumColumns = (p[K.albumColumns] ?: oldColumns).coerceIn(1, 4),
            groupColumns = (p[K.groupColumns] ?: oldColumns).coerceIn(1, 4),
            trackLayout = p[K.trackLayout] ?: d.trackLayout,
            favLayout = p[K.favLayout] ?: d.favLayout,
            songSort = (p[K.songSort] ?: d.songSort).coerceIn(0, SongSort.names.lastIndex),
            albumSort = (p[K.albumSort] ?: d.albumSort).coerceIn(0, AlbumSort.names.lastIndex),
            trackSort = (p[K.trackSort] ?: d.trackSort).coerceIn(0, TrackSort.names.lastIndex),
            groupsTitle = p[K.groupsTitle] ?: d.groupsTitle,
            homeLayout = p[K.homeLayout] ?: d.homeLayout,
            albumsLayout = p[K.albumsLayout] ?: d.albumsLayout,
            userName = p[K.userName] ?: d.userName,
            greeting = p[K.greeting] ?: d.greeting,
            greetingCustom = p[K.greetingCustom] ?: d.greetingCustom,
            onboarded = p[K.onboarded] ?: d.onboarded,
            lastBackup = p[K.lastBackup] ?: d.lastBackup,
            seeded = p[K.seeded] ?: d.seeded,
            coversAdopted = p[K.coversAdopted] ?: d.coversAdopted
        )
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setTheme(v: ThemeMode) = edit { it[K.theme] = v.name }
    suspend fun setAccent(v: Int) = edit { it[K.accent] = v.coerceIn(0, 3) }
    suspend fun setCustomAccent(argb: Int) = edit {
        it[K.accentCustom] = argb
        it[K.accent] = 3
    }
    suspend fun setUiStyle(v: Int) = edit { it[K.uiStyle] = v.coerceIn(0, 2) }
    suspend fun setTopAlbums(v: Int) = edit { it[K.topAlbums] = v.coerceIn(0, 8) }
    suspend fun setCardFace(v: Int) = edit { it[K.cardFace] = v.coerceIn(0, 2) }
    suspend fun setLastSong(id: Long) = edit { it[K.lastSongId] = id }
    suspend fun setNavRaised(v: Boolean) = edit { it[K.navRaised] = v }
    suspend fun setReading(lyrics: Int, chord: Int, spacing: Float, offset: Int, section: Int) = edit {
        it[K.lyricsSize] = lyrics
        it[K.chordSize] = chord
        it[K.lineSpacing] = spacing
        it[K.chordOffset] = offset
        it[K.sectionSize] = section
    }
    suspend fun setSectionStyle(v: Int) = edit { it[K.sectionStyle] = v }
    suspend fun setShowChords(v: Boolean) = edit { it[K.showChords] = v }
    suspend fun setShowTabs(v: Boolean) = edit { it[K.showTabs] = v }
    suspend fun setAutoScroll(v: Boolean) = edit { it[K.autoScroll] = v }
    suspend fun setScrollSpeed(v: Int) = edit { it[K.scrollSpeed] = v.coerceIn(1, 10) }
    suspend fun setShowEditButton(v: Boolean) = edit { it[K.showEditButton] = v }
    suspend fun setSongTheme(v: Int) = edit { it[K.songTheme] = v }
    suspend fun setLyricsView(v: Boolean) = edit { it[K.lyricsView] = v }
    suspend fun setSongsLayout(v: Int) = edit { it[K.songsLayout] = v.coerceIn(0, 4) }
    suspend fun setAlbumColumns(v: Int) = edit { it[K.albumColumns] = v.coerceIn(1, 4) }
    suspend fun setGroupColumns(v: Int) = edit { it[K.groupColumns] = v.coerceIn(1, 4) }
    suspend fun setTrackLayout(v: Int) = edit { it[K.trackLayout] = v }
    suspend fun setFavLayout(v: Int) = edit { it[K.favLayout] = v }
    suspend fun setSongSort(v: Int) = edit { it[K.songSort] = v }
    suspend fun setAlbumSort(v: Int) = edit { it[K.albumSort] = v }
    suspend fun setTrackSort(v: Int) = edit { it[K.trackSort] = v }
    suspend fun setGroupsTitle(v: String) = edit { it[K.groupsTitle] = v }
    suspend fun setHomeLayout(v: String) = edit { it[K.homeLayout] = v }
    suspend fun setAlbumsLayout(v: String) = edit { it[K.albumsLayout] = v }
    suspend fun setUserName(v: String) = edit { it[K.userName] = v }
    suspend fun setGreeting(style: Int, custom: String) = edit {
        it[K.greeting] = style
        it[K.greetingCustom] = custom
    }
    suspend fun setOnboarded(v: Boolean) = edit { it[K.onboarded] = v }
    suspend fun setLastBackup(v: Long) = edit { it[K.lastBackup] = v }
    suspend fun setSeeded(v: Boolean) = edit { it[K.seeded] = v }
    suspend fun setCoversAdopted(v: Boolean) = edit { it[K.coversAdopted] = v }

    /** Applies the settings saved inside a backup file. Keys that are missing are left alone. */
    suspend fun restore(o: org.json.JSONObject) = edit { p ->
        fun int(key: String, k: Preferences.Key<Int>) { if (o.has(key)) p[k] = o.optInt(key) }
        fun bool(key: String, k: Preferences.Key<Boolean>) { if (o.has(key)) p[k] = o.optBoolean(key) }
        fun str(key: String, k: Preferences.Key<String>) { if (o.has(key)) p[k] = o.optString(key) }
        str("userName", K.userName); str("greetingCustom", K.greetingCustom); str("theme", K.theme)
        str("groupsTitle", K.groupsTitle); str("homeLayout", K.homeLayout); str("albumsLayout", K.albumsLayout)
        int("greeting", K.greeting); int("accent", K.accent); int("accentCustom", K.accentCustom)
        int("uiStyle", K.uiStyle); int("topAlbums", K.topAlbums); int("cardFace", K.cardFace)
        int("lyricsSize", K.lyricsSize); int("chordSize", K.chordSize); int("chordOffset", K.chordOffset)
        int("sectionStyle", K.sectionStyle); int("sectionSize", K.sectionSize)
        int("scrollSpeed", K.scrollSpeed); int("songTheme", K.songTheme)
        int("gridColumns", K.gridColumns)
        int("songsLayout", K.songsLayout); int("albumColumns", K.albumColumns); int("groupColumns", K.groupColumns)
        int("trackLayout", K.trackLayout); int("favLayout", K.favLayout)
        int("songSort", K.songSort); int("albumSort", K.albumSort); int("trackSort", K.trackSort)
        if (o.has("lineSpacing")) p[K.lineSpacing] = o.optDouble("lineSpacing", 1.4).toFloat()
        bool("navRaised", K.navRaised); bool("showChords", K.showChords); bool("showTabs", K.showTabs)
        bool("showEditButton", K.showEditButton); bool("songsGrid", K.songsGrid)
    }
}
