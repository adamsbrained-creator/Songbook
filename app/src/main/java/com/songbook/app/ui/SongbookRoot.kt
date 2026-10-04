package com.songbook.app.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.songbook.app.applySystemBars
import com.songbook.app.data.SongEntity
import com.songbook.app.data.ThemeMode
import com.songbook.app.ui.components.NavBarHeight
import com.songbook.app.ui.components.SbBottomBar
import com.songbook.app.ui.components.SbFab
import com.songbook.app.ui.components.SongMenuSheet
import com.songbook.app.ui.components.TopDests
import com.songbook.app.ui.components.greetingLine
import com.songbook.app.ui.screens.AlbumScreen
import com.songbook.app.ui.screens.AlbumsScreen
import com.songbook.app.ui.screens.EditorScreen
import com.songbook.app.ui.screens.FavoritesScreen
import com.songbook.app.ui.screens.GroupScreen
import com.songbook.app.ui.screens.LibraryScreen
import com.songbook.app.ui.screens.NewSongRequest
import com.songbook.app.ui.screens.NewSongSheet
import com.songbook.app.ui.screens.OnboardingScreen
import com.songbook.app.ui.screens.SettingsScreen
import com.songbook.app.ui.screens.SongScreen
import com.songbook.app.ui.screens.WorkflowScreen
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SongbookTheme

@Composable
fun SongbookRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.theme) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
    val context = LocalContext.current
    (context as? ComponentActivity)?.let { activity ->
        LaunchedEffect(dark) { activity.applySystemBars(dark) }
    }

    // Wait for the saved settings so the intro / theme do not flash on every launch.
    if (!settingsLoaded) {
        Box(Modifier.fillMaxSize().background(if (systemDark) Color(0xFF0B0B0D) else Color(0xFFF5F5F7)))
        return
    }

    SongbookTheme(dark = dark, accent = settings.accent) {
        val c = Sb.colors

        if (!settings.onboarded) {
            OnboardingScreen(
                initialName = settings.userName,
                onDone = { name -> vm.edit { setUserName(name); setOnboarded(true) } }
            )
            return@SongbookTheme
        }

        val nav = rememberNavController()
        val backEntry by nav.currentBackStackEntryAsState()
        val route = backEntry?.destination?.route
        val topLevel = route != null && TopDests.any { it.route == route }
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        var newSong by remember { mutableStateOf<NewSongRequest?>(null) }
        var menuSongId by remember { mutableStateOf<Long?>(null) }
        val albumNames = remember(albums) { albums.associate { it.id to it.name } }

        fun navigateTop(dest: String) {
            nav.navigate(dest) {
                popUpTo("library") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(Modifier.fillMaxSize().background(c.bg)) {
            NavHost(nav, startDestination = "library", modifier = Modifier.fillMaxSize()) {
                composable("library") {
                    LibraryScreen(
                        songs = songs,
                        albums = albums,
                        greeting = greetingLine(settings),
                        songsGrid = settings.songsGrid,
                        columns = settings.gridColumns,
                        onOpenSong = { nav.navigate("song/$it") },
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onToggleFavorite = vm::toggleFavorite,
                        onMenu = { menuSongId = it },
                        onToggleLayout = { vm.edit { setSongsGrid(!settings.songsGrid) } },
                        onOpenWorkflow = { nav.navigate("workflow") }
                    )
                }
                composable("albums") {
                    AlbumsScreen(
                        albums = albums,
                        songs = songs,
                        groups = groups,
                        columns = settings.gridColumns,
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onOpenGroup = { nav.navigate("group/$it") },
                        onCreateAlbum = { vm.createAlbum(it) },
                        onCreateGroup = { name, ids -> vm.createGroup(name, ids) }
                    )
                }
                composable("favorites") {
                    FavoritesScreen(
                        songs = songs,
                        albums = albums,
                        onOpenSong = { nav.navigate("song/$it") },
                        onToggleFavorite = vm::toggleFavorite,
                        onMenu = { menuSongId = it }
                    )
                }
                composable("settings") {
                    SettingsScreen(settings, vm, onReplayIntro = { vm.edit { setOnboarded(false) } })
                }

                composable("workflow") {
                    WorkflowScreen(
                        songs = songs,
                        albums = albums,
                        onBack = { nav.popBackStack() },
                        onOpenSong = { nav.navigate("song/$it") },
                        onUpdateSong = vm::updateSongQuietly,
                        onNewIdea = { newSong = NewSongRequest(stage = 0) }
                    )
                }

                composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize().background(c.bg)) else AlbumScreen(
                        album = albums.firstOrNull { it.id == id },
                        songs = songs,
                        userName = settings.userName,
                        onBack = { nav.popBackStack() },
                        onOpenSong = { nav.navigate("song/$it") },
                        onToggleFavorite = vm::toggleFavorite,
                        onMenu = { menuSongId = it },
                        onAddSong = { newSong = NewSongRequest(it) },
                        onUpdateAlbum = vm::updateAlbum,
                        onDeleteAlbum = vm::deleteAlbum
                    )
                }

                composable("group/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize().background(c.bg)) else GroupScreen(
                        group = groups.firstOrNull { it.id == id },
                        albums = albums,
                        songs = songs,
                        columns = settings.gridColumns,
                        onBack = { nav.popBackStack() },
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onUpdateGroup = vm::updateGroup,
                        onSetMembers = { vm.setGroupMembers(id, it) },
                        onUngroup = { vm.ungroup(id) }
                    )
                }

                composable("song/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    val song = songs.firstOrNull { it.id == id }
                    if (!loaded) Box(Modifier.fillMaxSize().background(c.bg)) else SongScreen(
                        song = song,
                        album = albums.firstOrNull { it.id == song?.albumId },
                        settings = settings,
                        appDark = dark,
                        onClose = { nav.popBackStack() },
                        onMenu = { menuSongId = id },
                        onEdit = { nav.navigate("edit/$id") },
                        onToggleFavorite = { song?.let(vm::toggleFavorite) },
                        onCapo = { capo -> song?.let { vm.setCapo(it, capo) } },
                        onShowTabs = { vm.setShowTabs(it) },
                        onReading = { l, ch, s, off -> vm.setReading(l, ch, s, off) },
                        onLyricsView = { v -> vm.edit { setLyricsView(v) } },
                        onAutoScroll = { vm.setAutoScroll(it) },
                        onScrollSpeed = { v -> vm.edit { setScrollSpeed(v) } },
                        onSongTheme = { v -> vm.edit { setSongTheme(v) } }
                    )
                }

                composable(
                    "edit/{id}?new={new}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("new") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    val isNew = entry.arguments!!.getBoolean("new")
                    var song by remember { mutableStateOf<SongEntity?>(null) }
                    LaunchedEffect(id) { song = vm.getSong(id) }
                    EditorScreen(
                        song = song,
                        isNew = isNew,
                        albums = albums,
                        onClose = { nav.popBackStack() },
                        onDiscardNew = { vm.deleteSong(id); nav.popBackStack() },
                        onSave = { vm.saveSong(it) { nav.popBackStack() } },
                        onCreateAlbum = { name, done -> vm.createAlbum(name, onCreated = done) }
                    )
                }
            }

            if (topLevel) {
                SbBottomBar(
                    current = route,
                    raised = settings.navRaised,
                    onNavigate = ::navigateTop,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                SbFab(
                    raised = settings.navRaised,
                    onClick = { newSong = NewSongRequest() },
                    modifier = if (settings.navRaised) {
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp + navInset)
                    } else {
                        Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = NavBarHeight + navInset + 20.dp)
                    }
                )
            }
        }

        newSong?.let { request ->
            NewSongSheet(
                request = request,
                albums = albums,
                onDismiss = { newSong = null },
                onCreate = { input ->
                    newSong = null
                    vm.createSong(
                        SongEntity(
                            title = input.title,
                            albumId = input.albumId,
                            genre = input.genre,
                            tags = input.tags,
                            gradient = input.gradient,
                            body = input.body,
                            image = input.image,
                            stage = input.stage
                        )
                    ) { id -> nav.navigate("edit/$id?new=true") }
                },
                onCreateAlbum = { name, done -> vm.createAlbum(name, onCreated = done) }
            )
        }

        val menuSong = songs.firstOrNull { it.id == menuSongId }
        if (menuSong != null) {
            SongMenuSheet(
                song = menuSong,
                albumName = albumNames[menuSong.albumId],
                albums = albums,
                onDismiss = { menuSongId = null },
                onEdit = { nav.navigate("edit/${menuSong.id}") },
                onDuplicate = { vm.duplicateSong(menuSong) },
                onMove = { vm.moveToAlbum(menuSong, it) },
                onCreateAlbumAndMove = { name -> vm.createAlbum(name) { id -> vm.moveToAlbum(menuSong, id) } },
                onToggleFavorite = { vm.toggleFavorite(menuSong) },
                onArtwork = { g, img -> vm.setArtwork(menuSong, g, img) },
                onBadge = { t, col -> vm.setBadge(menuSong, t, col) },
                onStage = { vm.setStage(menuSong, it) },
                onExport = { context.shareSong(menuSong) },
                onDelete = { vm.deleteSong(menuSong.id) }
            )
        }
    }
}

private fun Context.shareSong(song: SongEntity) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, "${song.title}\n\n${song.body}")
    }
    startActivity(Intent.createChooser(send, "Export song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
