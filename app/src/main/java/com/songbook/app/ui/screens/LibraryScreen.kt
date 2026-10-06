package com.songbook.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.CardFace
import com.songbook.app.data.SongEntity
import com.songbook.app.data.SongLine
import com.songbook.app.data.SongParser
import com.songbook.app.data.Stage
import com.songbook.app.data.Transposer
import com.songbook.app.data.UiStyle
import com.songbook.app.data.hasChords
import com.songbook.app.data.hasTabs
import com.songbook.app.data.kindLabel
import com.songbook.app.ui.components.Artwork
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.CircleArtwork
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.FilterPill
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.GlassRoundButton
import com.songbook.app.ui.components.MomentsHeader
import com.songbook.app.ui.components.RatingTag
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.TopChips
import com.songbook.app.ui.components.WorkflowEntry
import com.songbook.app.ui.components.GlassTitle
import com.songbook.app.ui.components.rememberCover
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.components.songType
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.gradientBrush
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LibraryActions(
    val onOpenSong: (Long) -> Unit,
    val onOpenAlbum: (Long) -> Unit,
    val onToggleFavorite: (SongEntity) -> Unit,
    val onMenu: (Long) -> Unit,
    val onToggleLayout: () -> Unit,
    val onOpenWorkflow: () -> Unit,
    val onNew: () -> Unit,
    val onNavigate: (String) -> Unit,
    val onSettings: () -> Unit,
    val onCycleFace: () -> Unit
)

/** What the three layouts share: the filtered list and what is currently typed in the search box. */
private class Ctx(
    val songs: List<SongEntity>,
    val albums: List<AlbumEntity>,
    val albumNames: Map<Long, String>,
    val settings: AppSettings,
    val filtered: List<SongEntity>,
    val visible: List<SongEntity>,
    val searching: Boolean,
    val showAll: Boolean,
    val filter: Int,
    val query: String,
    val workflowCount: Int,
    val ideaCount: Int,
    val a: LibraryActions,
    val setFilter: (Int) -> Unit,
    val setQuery: (String) -> Unit,
    val toggleShowAll: () -> Unit
)

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    settings: AppSettings,
    greeting: String,
    workflowCount: Int,
    ideaCount: Int,
    searchTick: Int,
    actions: LibraryActions
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }

    val filtered = remember(songs, filter, query, albumNames) {
        songs.filter { s ->
            val kindOk = when (filter) {
                0 -> true
                1 -> !s.hasChords() && !s.hasTabs()
                2 -> s.hasChords()
                else -> s.hasTabs()
            }
            val q = query.trim()
            val queryOk = q.isEmpty() ||
                s.title.contains(q, true) || s.tags.contains(q, true) || s.body.contains(q, true) ||
                s.badge.contains(q, true) || s.typeName.contains(q, true) ||
                (albumNames[s.albumId]?.contains(q, true) == true)
            kindOk && queryOk
        }
    }
    val searching = query.isNotBlank() || filter != 0
    val visible = if (showAll || searching) filtered else filtered.take(5)
    val ctx = Ctx(
        songs, albums, albumNames, settings, filtered, visible, searching, showAll, filter, query,
        workflowCount, ideaCount, actions, { filter = it }, { query = it }, { showAll = !showAll }
    )
    when (Sb.style) {
        UiStyle.NEW -> NewLibrary(ctx, searchTick)
        UiStyle.GLASS -> GlassLibrary(ctx, greeting, searchTick)
        else -> OldLibrary(ctx, greeting, searchTick)
    }
}

// ================================================================================================
//  shared pieces
// ================================================================================================

@Composable
private fun SearchField(ctx: Ctx, focus: FocusRequester, modifier: Modifier = Modifier) {
    val c = Sb.colors
    Row(
        modifier.fillMaxWidth().height(48.dp).sbCard(16.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StrokeIcon(SbIcons.Search, c.mu, size = 20.dp)
        BasicTextField(
            value = ctx.query,
            onValueChange = ctx.setQuery,
            singleLine = true,
            textStyle = sbStyle(14, color = c.tx),
            cursorBrush = SolidColor(c.ac),
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (ctx.query.isEmpty()) Text("Search songs", style = sbStyle(14, color = c.mu))
                    inner()
                }
            }
        )
    }
}

@Composable
private fun FilterRow(ctx: Ctx, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (Sb.style == UiStyle.OLD) 4.dp else 8.dp)) {
        listOf("All", "Lyrics", "Chords", "Tabs").forEachIndexed { i, label ->
            if (Sb.style == UiStyle.OLD) FilterPill(label, ctx.filter == i) { ctx.setFilter(i) }
            else ChipPill(label, ctx.filter == i, { ctx.setFilter(i) }, Modifier.height(36.dp))
        }
    }
}

/** "All Songs" header with See All and the list / grid switch. */
private fun LazyListScope.songsHeader(ctx: Ctx) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(if (ctx.searching) "Results" else "All Songs")
            Row(verticalAlignment = Alignment.CenterVertically) {
                val c = Sb.colors
                if (!ctx.searching && ctx.filtered.size > 5) {
                    Text(
                        if (ctx.showAll) "Show Less" else "See All",
                        style = sbStyle(12, color = c.mu),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { ctx.toggleShowAll() }.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                SbIconButton(
                    icon = if (ctx.settings.songsGrid) SbIcons.Rows else SbIcons.Grid,
                    description = if (ctx.settings.songsGrid) "Show as list" else "Show as grid",
                    onClick = ctx.a.onToggleLayout,
                    tint = c.mu,
                    size = 20.dp
                )
            }
        }
    }
}

/** The song rows (or grid cells). Old: plain rows with dividers. New and Glass: every row is a card. */
private fun LazyListScope.songsBlock(ctx: Ctx, card: Boolean) {
    if (ctx.visible.isEmpty()) {
        item {
            EmptyHint(if (ctx.songs.isEmpty()) "No songs yet. Tap + to write your first one." else "Nothing matches your search.")
        }
    }
    if (ctx.settings.songsGrid) {
        val cols = ctx.settings.gridColumns
        val rows = ctx.visible.chunked(cols)
        items(rows, key = { row -> row.first().id }) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { s ->
                    SongGridCell(
                        s, ctx.albumNames[s.albumId] ?: "No album", cols >= 3,
                        { ctx.a.onOpenSong(s.id) }, { ctx.a.onToggleFavorite(s) }, { ctx.a.onMenu(s.id) },
                        Modifier.weight(1f)
                    )
                }
                repeat(cols - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    } else {
        items(ctx.visible, key = { it.id }) { s ->
            SongRow(
                title = s.title,
                subtitle = songType(s.typeName) + " · " + (ctx.albumNames[s.albumId] ?: "No album"),
                gradient = s.gradient, favorite = s.favorite,
                onClick = { ctx.a.onOpenSong(s.id) }, onFavorite = { ctx.a.onToggleFavorite(s) }, onMore = { ctx.a.onMenu(s.id) },
                image = s.image, badge = s.badge, badgeColor = s.badgeColor, rating = s.rating,
                divider = s.id != ctx.visible.last().id, card = card
            )
        }
    }
}

/** Horizontal tiles: recently edited songs. */
private fun LazyListScope.recentRow(ctx: Ctx, title: String, tile: Int) {
    if (ctx.songs.isEmpty() || ctx.query.isNotBlank() || ctx.filter != 0) return
    item { SectionTitle(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp)) }
    item {
        val c = Sb.colors
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(ctx.songs.take(8), key = { it.id }) { s ->
                Column(Modifier.width(tile.dp).clickable { ctx.a.onOpenSong(s.id) }) {
                    Box {
                        SquareArtwork(s.gradient, tile.dp, if (tile > 100) 20.dp else 18.dp, elevated = true, image = s.image)
                        if (s.badge.isNotBlank()) BadgePill(s.badge, s.badgeColor, Modifier.align(Alignment.TopStart).padding(6.dp), solid = true)
                    }
                    Text(s.title, style = sbStyle(14, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(s.kindLabel(), style = sbStyle(12, color = c.mu), maxLines = 1)
                    RatingTag(s.rating)
                }
            }
        }
    }
}

/** Square song tile for the grid layout. The heart and the menu sit on top of the artwork. */
@Composable
private fun SongGridCell(
    song: SongEntity,
    subtitle: String,
    compact: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Sb.colors
    Column(modifier.clickable(onClick = onClick)) {
        Artwork(song.gradient, RoundedCornerShape(if (compact) 18.dp else 22.dp), Modifier.fillMaxWidth().aspectRatio(1f), image = song.image) {
            if (song.badge.isNotBlank()) {
                Box(Modifier.matchParentSize().padding(8.dp), contentAlignment = Alignment.TopStart) { BadgePill(song.badge, song.badgeColor, solid = true) }
            }
            Box(Modifier.matchParentSize().padding(6.dp), contentAlignment = Alignment.BottomStart) {
                MiniButton(SbIcons.Heart, if (song.favorite) c.ac else Color.White, song.favorite, onFavorite)
            }
            Box(Modifier.matchParentSize().padding(6.dp), contentAlignment = Alignment.BottomEnd) {
                MiniButton(SbIcons.More, Color.White, false, onMore)
            }
        }
        Text(song.title, style = sbStyle(if (compact) 13 else 14, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
        RatingTag(song.rating)
    }
}

@Composable
private fun MiniButton(icon: com.songbook.app.ui.components.IconSpec, tint: Color, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.30f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { StrokeIcon(icon, tint, size = 18.dp, filled = filled) }
}

/** Top albums as a horizontal row of tiles (New and Glass). Hidden when the setting is 0. */
private fun LazyListScope.albumsRow(ctx: Ctx, title: String, tile: Int) {
    val n = ctx.settings.topAlbums
    if (n <= 0 || ctx.albums.isEmpty() || ctx.query.isNotBlank()) return
    item { SectionTitle(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp)) }
    item {
        val c = Sb.colors
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ctx.albums.take(n), key = { it.id }) { a ->
                Column(Modifier.width(tile.dp).clickable { ctx.a.onOpenAlbum(a.id) }) {
                    Artwork(a.gradient, RoundedCornerShape(18.dp), Modifier.size(tile.dp), image = a.image) {
                        if (a.badge.isNotBlank()) Box(Modifier.matchParentSize().padding(6.dp), contentAlignment = Alignment.TopStart) { BadgePill(a.badge, a.badgeColor, solid = true) }
                    }
                    Text(a.name, style = sbStyle(12, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ================================================================================================
//  OLD: the 1.x layout
// ================================================================================================

@Composable
private fun OldLibrary(ctx: Ctx, greeting: String, searchTick: Int) {
    val c = Sb.colors
    val focus = remember { FocusRequester() }
    LazyColumn(Modifier.fillMaxSize().screenBg().statusBarsPadding(), contentPadding = PaddingValues(bottom = 130.dp)) {
        item {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                Text(greeting, style = sbStyle(26, FontWeight.Bold, c.tx))
                Text("Your songs, all in one place.", style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
            }
        }
        item { FilterRow(ctx, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) }
        item { SearchField(ctx, focus, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) }
        val n = ctx.settings.topAlbums
        if (n > 0 && ctx.albums.isNotEmpty() && ctx.query.isBlank()) {
            item {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ctx.albums.take(n).chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { a ->
                                val cover = rememberCover(a.image, 400)
                                Box(
                                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(gradientBrush(a.gradient))
                                        .clickable { ctx.a.onOpenAlbum(a.id) }
                                ) {
                                    if (cover != null) {
                                        Image(cover, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
                                    }
                                    Text(
                                        a.name, style = sbStyle(15, FontWeight.SemiBold, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (ctx.query.isBlank() && ctx.filter == 0 && ctx.workflowCount + ctx.songs.size > 0) {
            item { WorkflowEntry(ctx.workflowCount, ctx.ideaCount, ctx.a.onOpenWorkflow, Modifier.padding(top = 16.dp)) }
        }
        recentRow(ctx, "Recently Edited", 112)
        songsHeader(ctx)
        songsBlock(ctx, card = false)
    }
}

// ================================================================================================
//  GLASS
// ================================================================================================

@Composable
private fun GlassLibrary(ctx: Ctx, greeting: String, searchTick: Int) {
    val focus = remember { FocusRequester() }
    var menu by remember { mutableStateOf(false) }
    val c = Sb.colors
    LazyColumn(Modifier.fillMaxSize().screenBg().statusBarsPadding(), contentPadding = PaddingValues(bottom = 130.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                GlassTitle(greeting, "Your songs, all in one place.", Modifier.weight(1f).padding(end = 12.dp))
                Box {
                    GlassRoundButton(SbIcons.More, "More", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(c.sheet)) {
                        DropdownMenuItem(
                            text = { Text(if (ctx.settings.songsGrid) "Show as list" else "Show as grid", style = sbStyle(14, color = c.tx)) },
                            onClick = { menu = false; ctx.a.onToggleLayout() }
                        )
                        DropdownMenuItem(text = { Text("Workflow", style = sbStyle(14, color = c.tx)) }, onClick = { menu = false; ctx.a.onOpenWorkflow() })
                    }
                }
            }
        }
        item { FilterRow(ctx, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) }
        item { SearchField(ctx, focus, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)) }
        albumsRow(ctx, "Pinned", 92)
        if (ctx.query.isBlank() && ctx.filter == 0) {
            item { WorkflowEntry(ctx.workflowCount, ctx.ideaCount, ctx.a.onOpenWorkflow, Modifier.padding(top = 16.dp)) }
        }
        recentRow(ctx, "Recently Edited", 92)
        songsHeader(ctx)
        songsBlock(ctx, card = true)
    }
}

// ================================================================================================
//  NEW: story circles and big swipeable song cards
// ================================================================================================

private fun SongEntity.cardChords(): List<String> {
    val shift = if (capoAdjust && capo > 0) -capo else 0
    return SongParser.parse(body).filterIsInstance<SongLine.Lyric>().flatMap { l -> l.chords.map { Transposer.chord(it.name, shift) } }.take(8)
}

private fun SongEntity.cardLyrics(): List<String> =
    SongParser.parse(body).filterIsInstance<SongLine.Lyric>().map { it.text.trim() }.filter { it.isNotEmpty() }.take(4)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewLibrary(ctx: Ctx, searchTick: Int) {
    val c = Sb.colors
    val focus = remember { FocusRequester() }
    var showSearch by remember { mutableStateOf(false) }
    LaunchedEffect(searchTick) {
        if (searchTick > 0) {
            showSearch = true
            delay(120)
            runCatching { focus.requestFocus() }
        }
    }
    val stories = remember(ctx.songs) { ctx.songs.take(10) }
    val pager = rememberPagerState(pageCount = { stories.size.coerceAtLeast(1) })
    val page = pager.currentPage

    LazyColumn(Modifier.fillMaxSize().screenBg().statusBarsPadding(), contentPadding = PaddingValues(bottom = 130.dp)) {
        item { MomentsHeader("Latest Songs", onSettings = ctx.a.onSettings, onPlus = ctx.a.onNew) }
        item { TopChips("library", ctx.a.onNavigate, Modifier.padding(bottom = 10.dp)) }
        if (showSearch || ctx.query.isNotBlank()) {
            item { SearchField(ctx, focus, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) }
        }
        if (stories.isNotEmpty() && ctx.query.isBlank()) {
            item {
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(stories.size) { i ->
                        val s = stories[i]
                        val on = i == page
                        Column(
                            Modifier.clip(RoundedCornerShape(30.dp)).clickable { scope.launch { pager.animateScrollToPage(i) } },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(60.dp).then(if (on) Modifier.border(2.dp, c.tx, CircleShape) else Modifier).padding(3.dp)) {
                                CircleArtwork(s.gradient, 54.dp, image = s.image)
                            }
                            Box(
                                Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape)
                                    .background(if (s.stage >= Stage.DONE) (if (on) c.tx else c.mu) else c.ac)
                            )
                        }
                    }
                }
            }
            item {
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 10.dp,
                    modifier = Modifier.padding(top = 14.dp)
                ) { i ->
                    val s = stories[i]
                    MomentCard(s, ctx.settings.cardFace, { ctx.a.onOpenSong(s.id) }, ctx.a.onCycleFace)
                }
            }
        }
        item { FilterRow(ctx, Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp)) }
        if (ctx.query.isBlank() && ctx.filter == 0) {
            item { WorkflowEntry(ctx.workflowCount, ctx.ideaCount, ctx.a.onOpenWorkflow, Modifier.padding(top = 14.dp)) }
        }
        albumsRow(ctx, "Albums", 100)
        songsHeader(ctx)
        songsBlock(ctx, card = true)
    }
}

/** One big card: artwork, lyrics or chords on the song's color, with a small player-style panel. */
@Composable
private fun MomentCard(song: SongEntity, face: Int, onOpen: () -> Unit, onCycle: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val cover = rememberCover(song.image, 900)
    val lyrics = remember(song.body) { song.cardLyrics() }
    val chords = remember(song.body, song.capo, song.capoAdjust) { song.cardChords() }
    Box(Modifier.fillMaxWidth().height(390.dp).clip(shape).background(gradientBrush(song.gradient)).clickable(onClick = onOpen)) {
        if (cover != null) Image(cover, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        if (cover != null && face != CardFace.ARTWORK) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))

        when (face) {
            CardFace.LYRICS -> Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp)) {
                if (lyrics.isEmpty()) Text("No lyrics yet", style = sbStyle(24, FontWeight.Bold, Color.White.copy(alpha = 0.6f)))
                lyrics.forEachIndexed { i, line ->
                    Text(
                        line,
                        style = sbStyle(if (i == 0) 27 else 24, FontWeight.Bold, Color.White.copy(alpha = if (i == 0) 1f else 0.5f)),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
            CardFace.CHORDS -> Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp)) {
                Text("Chords", style = sbStyle(12, FontWeight.SemiBold, Color.White.copy(alpha = 0.7f)))
                if (chords.isEmpty()) Text("No chords yet", style = sbStyle(24, FontWeight.Bold, Color.White.copy(alpha = 0.6f)), modifier = Modifier.padding(top = 8.dp))
                chords.chunked(2).forEachIndexed { r, pair ->
                    Row(Modifier.padding(top = 6.dp)) {
                        pair.forEach { ch ->
                            Text(
                                ch,
                                style = sbStyle(36, FontWeight.Bold, Color.White.copy(alpha = if (r < 2) 1f else 0.5f)),
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                }
                if (song.capo > 0) {
                    Text(
                        if (song.capoAdjust) "Capo ${song.capo} · shapes" else "Capo ${song.capo}",
                        style = sbStyle(12, FontWeight.SemiBold, Color.White),
                        modifier = Modifier.padding(top = 18.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            else -> if (cover == null) {
                Text(
                    song.title,
                    style = sbStyle(40, FontWeight.Bold, Color.White).copy(fontStyle = FontStyle.Italic),
                    maxLines = 3,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp, end = 24.dp, bottom = 80.dp)
                )
            }
        }

        // player-style panel
        Column(
            Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.42f)).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = sbStyle(15, FontWeight.Bold, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = buildString {
                        append(songType(song.typeName))
                        if (song.genre.isNotBlank()) append(" · ").append(song.genre)
                        if (song.capo > 0) append(" · Capo ").append(song.capo)
                    }
                    Text(sub, style = sbStyle(12, color = Color.White.copy(alpha = 0.75f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onCycle),
                    contentAlignment = Alignment.Center
                ) { StrokeIcon(SbIcons.Grid, Color.White, size = 17.dp) }
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Stage.name(song.stage), style = sbStyle(10, color = Color.White.copy(alpha = 0.8f)))
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f))) {
                    Box(Modifier.fillMaxWidth(song.stage.coerceIn(0, Stage.DONE) / Stage.DONE.toFloat().coerceAtLeast(1f)).height(4.dp).background(Color.White, CircleShape))
                }
                Text("Finished", style = sbStyle(10, color = Color.White.copy(alpha = 0.8f)))
            }
        }
    }
}
