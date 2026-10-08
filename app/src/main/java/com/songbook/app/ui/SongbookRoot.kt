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
import androidx.compose.runtime.mutableIntStateOf
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
import com.songbook.app.data.AlbumSort
import com.songbook.app.data.SongEntity
import com.songbook.app.data.SongSort
import com.songbook.app.data.TrackSort
import com.songbook.app.data.inAlbumOrder
import com.songbook.app.data.sideList
import com.songbook.app.data.sortedAlbums
import com.songbook.app.data.sortedGroups
import com.songbook.app.data.sortedSongs
import com.songbook.app.data.withAlbumCover
import com.songbook.app.data.Stage
import com.songbook.app.data.ThemeMode
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.components.GlassBackdrop
import com.songbook.app.ui.components.GlassTabBar
import com.songbook.app.ui.components.MomentsBar
import com.songbook.app.ui.components.NavBarHeight
import com.songbook.app.ui.components.SbBottomBar
import com.songbook.app.ui.components.SbFab
import com.songbook.app.ui.components.SongMenuActions
import com.songbook.app.ui.components.SongMenuSheet
import com.songbook.app.ui.components.TopDests
import com.songbook.app.ui.components.greetingLine
import com.songbook.app.ui.components.songType
import com.songbook.app.ui.screens.AlbumActions
import com.songbook.app.ui.screens.AlbumScreen
import com.songbook.app.ui.screens.AlbumsActions
import com.songbook.app.ui.screens.ArrangeItem
import com.songbook.app.ui.screens.ArrangeScreen
import com.songbook.app.ui.screens.GroupActions
import com.songbook.app.ui.screens.AlbumsScreen
import com.songbook.app.ui.screens.DeskScreen
import com.songbook.app.ui.screens.EditorScreen
import com.songbook.app.ui.screens.FavoritesScreen
import com.songbook.app.ui.screens.GroupScreen
import com.songbook.app.ui.screens.LibraryActions
import com.songbook.app.ui.screens.LibraryScreen
import com.songbook.app.ui.screens.NewSongRequest
import com.songbook.app.ui.screens.NewSongSheet
import com.songbook.app.ui.screens.OnboardingScreen
import com.songbook.app.ui.screens.ReaderActions
import com.songbook.app.ui.screens.SettingsScreen
import com.songbook.app.ui.screens.SongScreen
import com.songbook.app.ui.screens.WorkflowScreen
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.SongbookTheme

@Composable
fun SongbookRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
    val dbSongs by vm.songs.collectAsStateWithLifecycle()
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

    SongbookTheme(dark = dark, accent = settings.accent, accentCustom = settings.accentCustom, style = settings.uiStyle) {
        val c = Sb.colors

        if (!settings.onboarded) {
            Box(Modifier.fillMaxSize()) {
                if (settings.uiStyle == UiStyle.GLASS) GlassBackdrop()
                OnboardingScreen(
                    initialName = settings.userName,
                    onDone = { name -> vm.edit { setUserName(name); setOnboarded(true) } }
                )
            }
            return@SongbookTheme
        }

        val nav = rememberNavController()
        val backEntry by nav.currentBackStackEntryAsState()
        val route = backEntry?.destination?.route
        val topLevel = route != null && TopDests.any { it.route == route }
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        var newSong by remember { mutableStateOf<NewSongRequest?>(null) }
        var menuSongId by remember { mutableStateOf<Long?>(null) }
        var searchTick by remember { mutableIntStateOf(0) }
        val albumNames = remember(albums) { albums.associate { it.id to it.name } }

        // Songs are drawn with the cover of their album (unless the cover was chosen by hand). These copies are
        // only for drawing; everything that saves goes through the view model, which reads the stored song.
        val albumsById = remember(albums) { albums.associateBy { it.id } }
        val allSongs = remember(dbSongs, albumsById) { dbSongs.map { it.withAlbumCover(albumsById) } }
        // Ideas live on the Workflow page only; every other screen sees the songs without them.
        val songs = remember(allSongs) { allSongs.filter { it.stage != Stage.IDEA } }
        val workflowCount = remember(allSongs) { allSongs.count { it.stage < Stage.DONE } }
        val ideaCount = remember(allSongs) { allSongs.count { it.stage == Stage.IDEA } }
        val lastSong = remember(allSongs, settings.lastSongId) {
            songs.firstOrNull { it.id == settings.lastSongId } ?: songs.firstOrNull()
        }

        fun navigateTop(dest: String) {
            nav.navigate(dest) {
                popUpTo("library") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        fun openSong(id: Long) {
            vm.edit { setLastSong(id) }
            nav.navigate("song/$id")
        }

        Box(Modifier.fillMaxSize().background(c.bg)) {
            if (Sb.style == UiStyle.GLASS) GlassBackdrop()
            NavHost(nav, startDestination = "library", modifier = Modifier.fillMaxSize()) {
                composable("library") {
                    LibraryScreen(
                        songs = songs,
                        albums = albums,
                        settings = settings,
                        greeting = greetingLine(settings),
                        workflowCount = workflowCount,
                        ideaCount = ideaCount,
                        searchTick = searchTick,
                        actions = LibraryActions(
                            onOpenSong = ::openSong,
                            onOpenAlbum = { nav.navigate("album/$it") },
                            onToggleFavorite = vm::toggleFavorite,
                            onMenu = { menuSongId = it },
                            onOpenWorkflow = { nav.navigate("workflow") },
                            onNew = { newSong = NewSongRequest() },
                            onNavigate = ::navigateTop,
                            onSettings = { navigateTop("settings") },
                            onCycleFace = { vm.edit { setCardFace((settings.cardFace + 1) % 3) } },
                            onSongSort = { v -> vm.edit { setSongSort(v) } },
                            onSongsLayout = { v -> vm.edit { setSongsLayout(v) } },
                            onArrangeSongs = { nav.navigate("arrange/songs/0") },
                            onSaveHome = { v -> vm.edit { setHomeLayout(v) } }
                        )
                    )
                }
                composable("albums") {
                    AlbumsScreen(
                        albums = albums,
                        songs = songs,
                        groups = groups,
                        settings = settings,
                        workflowCount = workflowCount,
                        ideaCount = ideaCount,
                        actions = AlbumsActions(
                            onOpenAlbum = { nav.navigate("album/$it") },
                            onOpenGroup = { nav.navigate("group/$it") },
                            onCreateAlbum = { vm.createAlbum(it) },
                            onCreateGroup = { name, ids -> vm.createGroup(name, ids) },
                            onOpenWorkflow = { nav.navigate("workflow") },
                            onNavigate = ::navigateTop,
                            onSettings = { navigateTop("settings") },
                            onAlbumSort = { v -> vm.edit { setAlbumSort(v) } },
                            onAlbumColumns = { v -> vm.edit { setAlbumColumns(v) } },
                            onGroupColumns = { v -> vm.edit { setGroupColumns(v) } },
                            onArrange = { kind -> nav.navigate("arrange/$kind/0") },
                            onSaveLayout = { v -> vm.edit { setAlbumsLayout(v) } }
                        )
                    )
                }
                composable("favorites") {
                    FavoritesScreen(
                        songs = songs,
                        albums = albums,
                        onOpenSong = ::openSong,
                        onToggleFavorite = vm::toggleFavorite,
                        onMenu = { menuSongId = it },
                        onNavigate = ::navigateTop,
                        onSettings = { navigateTop("settings") },
                        onNew = { newSong = NewSongRequest() },
                        layout = settings.favLayout
                    )
                }
                composable("settings") {
                    SettingsScreen(settings, vm, onReplayIntro = { vm.edit { setOnboarded(false) } })
                }

                composable("workflow") {
                    WorkflowScreen(
                        songs = allSongs,
                        albums = albums,
                        onBack = { nav.popBackStack() },
                        onOpenSong = ::openSong,
                        onOpenDesk = { nav.navigate("desk/$it") },
                        onPatch = { id, change -> vm.patchSong(id, change = change) },
                        onNewIdea = { newSong = NewSongRequest(stage = Stage.IDEA) }
                    )
                }

                composable("desk/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize()) else DeskScreen(
                        song = dbSongs.firstOrNull { it.id == id },
                        onBack = { nav.popBackStack() },
                        onSave = { vm.saveSong(it) },
                        onEditText = { nav.navigate("edit/$it") }
                    )
                }

                composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize()) else AlbumScreen(
                        album = albums.firstOrNull { it.id == id },
                        songs = songs,
                        settings = settings,
                        userName = settings.userName,
                        actions = AlbumActions(
                            onBack = { nav.popBackStack() },
                            onOpenSong = ::openSong,
                            onToggleFavorite = vm::toggleFavorite,
                            onMenu = { menuSongId = it },
                            onAddSong = { newSong = NewSongRequest(it) },
                            onUpdateAlbum = vm::updateAlbum,
                            onDeleteAlbum = vm::deleteAlbum,
                            onTrackLayout = { v -> vm.edit { setTrackLayout(v) } },
                            onTrackSort = { v -> vm.edit { setTrackSort(v) } },
                            onArrange = { nav.navigate("arrange/tracks/$it") },
                            onSetSides = { album, names -> vm.setAlbumSides(album, names) },
                            onRemoveSide = { album, index -> vm.removeAlbumSide(album, index) },
                            onFollowCover = vm::followAlbumCover
                        )
                    )
                }

                composable("group/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize()) else GroupScreen(
                        group = groups.firstOrNull { it.id == id },
                        albums = albums,
                        songs = songs,
                        settings = settings,
                        actions = GroupActions(
                            onBack = { nav.popBackStack() },
                            onOpenAlbum = { nav.navigate("album/$it") },
                            onUpdateGroup = vm::updateGroup,
                            onSetMembers = { vm.setGroupMembers(id, it) },
                            onUngroup = { vm.ungroup(id) },
                            onArrange = { nav.navigate("arrange/members/$id") }
                        )
                    )
                }

                // drag songs, albums and groups into your own order
                composable(
                    "arrange/{kind}/{id}",
                    arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val kind = entry.arguments!!.getString("kind") ?: "songs"
                    val id = entry.arguments!!.getLong("id")
                    if (!loaded) Box(Modifier.fillMaxSize()) else {
                        val memberCounts = albums.groupingBy { it.groupId }.eachCount()
                        val groupIds = groups.map { it.id }.toSet()
                        val loose = albums.filter { it.groupId == null || it.groupId !in groupIds }
                        fun albumItem(a: com.songbook.app.data.AlbumEntity) =
                            ArrangeItem(a.id, a.name, a.typeName.ifBlank { "Album" } + " · " + a.year, a.gradient, a.image)
                        val sides: List<String> = if (kind == "tracks") (albums.firstOrNull { it.id == id }?.sideList ?: emptyList()) else emptyList()
                        val items: List<ArrangeItem> = when (kind) {
                            "tracks" -> songs.filter { it.albumId == id }.inAlbumOrder().map {
                                ArrangeItem(it.id, it.title, songType(it.typeName), it.gradient, it.image, it.side)
                            }
                            "albums" -> loose.sortedAlbums(AlbumSort.MANUAL).map(::albumItem)
                            "members" -> albums.filter { it.groupId == id }.sortedAlbums(AlbumSort.MANUAL).map(::albumItem)
                            "groups" -> groups.sortedGroups(AlbumSort.MANUAL).map {
                                ArrangeItem(it.id, it.name, it.typeName.ifBlank { "Group" } + " · " + (memberCounts[it.id] ?: 0) + " albums", it.gradient, it.image)
                            }
                            else -> songs.sortedSongs(SongSort.MANUAL, albumNames).map {
                                ArrangeItem(it.id, it.title, albumNames[it.albumId] ?: "No album", it.gradient, it.image)
                            }
                        }
                        ArrangeScreen(
                            title = when (kind) { "tracks" -> "Arrange songs"; "albums", "members" -> "Arrange albums"; "groups" -> "Arrange groups"; else -> "Arrange songs" },
                            items = items,
                            sides = sides,
                            onCancel = { nav.popBackStack() },
                            onDone = { ids ->
                                when (kind) {
                                    "tracks" -> { vm.arrange(ArrangeKind.TRACKS, ids); vm.edit { setTrackSort(TrackSort.ALBUM) } }
                                    "albums", "members" -> { vm.arrange(ArrangeKind.ALBUMS, ids); vm.edit { setAlbumSort(AlbumSort.MANUAL) } }
                                    "groups" -> { vm.arrange(ArrangeKind.GROUPS, ids); vm.edit { setAlbumSort(AlbumSort.MANUAL) } }
                                    else -> { vm.arrange(ArrangeKind.SONGS, ids); vm.edit { setSongSort(SongSort.MANUAL) } }
                                }
                                nav.popBackStack()
                            }
                        )
                    }
                }

                composable("song/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments!!.getLong("id")
                    val song = allSongs.firstOrNull { it.id == id }
                    if (!loaded) Box(Modifier.fillMaxSize()) else SongScreen(
                        song = song,
                        album = albums.firstOrNull { it.id == song?.albumId },
                        settings = settings,
                        appDark = dark,
                        actions = ReaderActions(
                            onClose = { nav.popBackStack() },
                            onMenu = { menuSongId = id },
                            onEdit = { nav.navigate("edit/$id") },
                            onToggleFavorite = { song?.let(vm::toggleFavorite) },
                            onCapo = { capo, adjust -> song?.let { vm.setCapo(it, capo, adjust) } },
                            onShowTabs = { vm.setShowTabs(it) },
                            onReading = { l, ch, sp, off, sec -> vm.setReading(l, ch, sp, off, sec) },
                            onLyricsView = { v -> vm.edit { setLyricsView(v) } },
                            onAutoScroll = { vm.setAutoScroll(it) },
                            onScrollSpeed = { v -> vm.edit { setScrollSpeed(v) } },
                            onSongTheme = { v -> vm.edit { setSongTheme(v) } },
                            onRate = { r -> song?.let { vm.setRating(it, r) } },
                            onBody = { body -> vm.setBody(id, body) }
                        )
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
                when (Sb.style) {
                    UiStyle.GLASS -> GlassTabBar(
                        current = route,
                        onNavigate = ::navigateTop,
                        onNew = { newSong = NewSongRequest() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    UiStyle.NEW -> MomentsBar(
                        last = lastSong,
                        lastSubtitle = lastSong?.let { s ->
                            songType(s.typeName) + if (s.capo > 0) " · Capo ${s.capo}" else ""
                        } ?: "",
                        onHome = { navigateTop("library") },
                        onOpenLast = { lastSong?.let { openSong(it.id) } },
                        onSearch = { navigateTop("library"); searchTick++ },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    else -> {
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
                            stage = input.stage,
                            typeName = input.typeName,
                            coverManual = input.coverManual
                        )
                    ) { id ->
                        // ideas start on the songwriting desk, songs in the editor
                        if (input.stage == Stage.IDEA) nav.navigate("desk/$id") else nav.navigate("edit/$id?new=true")
                    }
                },
                onCreateAlbum = { name, done -> vm.createAlbum(name, onCreated = done) }
            )
        }

        val menuSong = allSongs.firstOrNull { it.id == menuSongId }
        if (menuSong != null) {
            SongMenuSheet(
                song = menuSong,
                albumName = albumNames[menuSong.albumId],
                albums = albums,
                sides = albums.firstOrNull { it.id == menuSong.albumId }?.sideList ?: emptyList(),
                actions = SongMenuActions(
                    onEdit = { nav.navigate("edit/${menuSong.id}") },
                    onDesk = { nav.navigate("desk/${menuSong.id}") },
                    onDuplicate = { vm.duplicateSong(menuSong) },
                    onMove = { vm.moveToAlbum(menuSong, it) },
                    onCreateAlbumAndMove = { name -> vm.createAlbum(name) { id -> vm.moveToAlbum(menuSong, id) } },
                    onToggleFavorite = { vm.toggleFavorite(menuSong) },
                    onArtwork = { g, img -> vm.setArtwork(menuSong, g, img) },
                    onBadge = { t, col -> vm.setBadge(menuSong, t, col) },
                    onStage = { vm.setStage(menuSong, it) },
                    onRate = { vm.setRating(menuSong, it) },
                    onType = { vm.setType(menuSong, it) },
                    onSide = { vm.setSide(menuSong, it) },
                    onUseAlbumCover = { vm.useAlbumCover(menuSong) },
                    onExport = { context.shareSong(menuSong) },
                    onDelete = { vm.deleteSong(menuSong.id) }
                ),
                onDismiss = { menuSongId = null }
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
