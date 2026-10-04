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
import com.songbook.app.ui.screens.AlbumScreen
import com.songbook.app.ui.screens.AlbumsScreen
import com.songbook.app.ui.screens.EditorScreen
import com.songbook.app.ui.screens.FavoritesScreen
import com.songbook.app.ui.screens.LibraryScreen
import com.songbook.app.ui.screens.NewSongRequest
import com.songbook.app.ui.screens.NewSongSheet
import com.songbook.app.ui.screens.SettingsScreen
import com.songbook.app.ui.screens.SongScreen
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SongbookTheme

@Composable
fun SongbookRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
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

    SongbookTheme(dark = dark, accent = settings.accent) {
        val c = Sb.colors
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
                        userName = settings.userName,
                        onOpenSong = { nav.navigate("song/$it") },
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onToggleFavorite = vm::toggleFavorite,
                        onMenu = { menuSongId = it }
                    )
                }
                composable("albums") {
                    AlbumsScreen(
                        albums = albums,
                        songs = songs,
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onCreateAlbum = { vm.createAlbum(it) }
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
                composable("settings") { SettingsScreen(settings, vm) }

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
                        onShowChords = { vm.setShowChords(it) },
                        onShowTabs = { vm.setShowTabs(it) },
                        onReading = { l, ch, s -> vm.setReading(l, ch, s) }
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
                            body = input.body
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
                onArtwork = { vm.setGradient(menuSong, it) },
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
