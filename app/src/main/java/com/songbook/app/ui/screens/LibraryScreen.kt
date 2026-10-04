package com.songbook.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.Stage
import com.songbook.app.data.hasChords
import com.songbook.app.data.hasTabs
import com.songbook.app.data.kindLabel
import com.songbook.app.ui.components.Artwork
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.FilterPill
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.StrokeIcon
import com.songbook.app.ui.components.rememberCover
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.gradientBrush
import com.songbook.app.ui.theme.sbStyle

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    greeting: String,
    songsGrid: Boolean,
    columns: Int,
    onOpenSong: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onMenu: (Long) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenWorkflow: () -> Unit
) {
    val c = Sb.colors
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }
    val inProgress = remember(songs) { songs.count { it.stage < Stage.DONE } }

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
                s.badge.contains(q, true) ||
                (albumNames[s.albumId]?.contains(q, true) == true)
            kindOk && queryOk
        }
    }
    val searching = query.isNotBlank() || filter != 0
    val visible = if (showAll || searching) filtered else filtered.take(5)

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                Text(greeting, style = sbStyle(26, FontWeight.Bold, c.tx))
                Text("Your songs, all in one place.", style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
            }
        }
        item {
            Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("All", "Lyrics", "Chords", "Tabs").forEachIndexed { i, label ->
                    FilterPill(label, filter == i) { filter = i }
                }
            }
        }
        item {
            Row(
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .sbCard(16.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StrokeIcon(SbIcons.Search, c.mu, size = 20.dp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = sbStyle(14, color = c.tx),
                    cursorBrush = SolidColor(c.ac),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("Search songs", style = sbStyle(14, color = c.mu))
                            inner()
                        }
                    }
                )
            }
        }
        if (albums.isNotEmpty() && query.isBlank()) {
            item {
                val shown = albums.take(4)
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shown.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { a ->
                                val cover = rememberCover(a.image, 400)
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(gradientBrush(a.gradient))
                                        .clickable { onOpenAlbum(a.id) }
                                ) {
                                    if (cover != null) {
                                        Image(cover, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
                                    }
                                    Text(
                                        a.name,
                                        style = sbStyle(15, FontWeight.SemiBold, Color.White),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
        if (query.isBlank() && filter == 0 && songs.isNotEmpty()) {
            item { WorkflowCard(inProgress, onOpenWorkflow) }
        }
        if (songs.isNotEmpty() && query.isBlank() && filter == 0) {
            item { SectionTitle("Recently Edited", Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(songs.take(8), key = { it.id }) { s ->
                        Column(Modifier.width(112.dp).clickable { onOpenSong(s.id) }) {
                            Box {
                                SquareArtwork(s.gradient, 112.dp, 20.dp, elevated = true, image = s.image)
                                if (s.badge.isNotBlank()) {
                                    BadgePill(s.badge, s.badgeColor, Modifier.align(Alignment.TopStart).padding(6.dp), solid = true)
                                }
                            }
                            Text(s.title, style = sbStyle(14, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            Text(s.kindLabel(), style = sbStyle(12, color = c.mu), maxLines = 1)
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(if (searching) "Results" else "All Songs")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!searching && filtered.size > 5) {
                        Text(
                            if (showAll) "Show Less" else "See All",
                            style = sbStyle(12, color = c.mu),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showAll = !showAll }.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    SbIconButton(
                        icon = if (songsGrid) SbIcons.Rows else SbIcons.Grid,
                        description = if (songsGrid) "Show as list" else "Show as grid",
                        onClick = onToggleLayout,
                        tint = c.mu,
                        size = 20.dp
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyHint(
                    if (songs.isEmpty()) "No songs yet. Tap + to write your first one."
                    else "Nothing matches your search."
                )
            }
        }
        if (songsGrid) {
            val rows = visible.chunked(columns)
            items(rows, key = { row -> row.first().id }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { s ->
                        SongGridCell(
                            song = s,
                            subtitle = albumNames[s.albumId] ?: "No album",
                            compact = columns >= 3,
                            onClick = { onOpenSong(s.id) },
                            onFavorite = { onToggleFavorite(s) },
                            onMore = { onMenu(s.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        } else {
            items(visible, key = { it.id }) { s ->
                SongRow(
                    title = s.title,
                    subtitle = albumNames[s.albumId] ?: "No album",
                    gradient = s.gradient,
                    favorite = s.favorite,
                    onClick = { onOpenSong(s.id) },
                    onFavorite = { onToggleFavorite(s) },
                    onMore = { onMenu(s.id) },
                    image = s.image,
                    badge = s.badge,
                    badgeColor = s.badgeColor,
                    divider = s.id != visible.last().id
                )
            }
        }
    }
}

/** Entry to the Workflow page: how many songs are still being written. */
@Composable
private fun WorkflowCard(inProgress: Int, onClick: () -> Unit) {
    val c = Sb.colors
    Row(
        Modifier
            .padding(start = 24.dp, end = 24.dp, top = 16.dp)
            .fillMaxWidth()
            .height(60.dp)
            .sbCard(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(c.ac.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Text("$inProgress", style = sbStyle(14, FontWeight.Bold, c.ac)) }
        Column(Modifier.weight(1f)) {
            Text("Workflow", style = sbStyle(14, FontWeight.SemiBold, c.tx))
            Text(
                if (inProgress == 0) "Everything is finished" else if (inProgress == 1) "1 song still in the making" else "$inProgress songs still in the making",
                style = sbStyle(12, color = c.mu)
            )
        }
        StrokeIcon(SbIcons.ChevronRight, c.mu, size = 20.dp)
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
        Artwork(
            song.gradient, RoundedCornerShape(if (compact) 18.dp else 22.dp),
            Modifier.fillMaxWidth().aspectRatio(1f),
            image = song.image
        ) {
            if (song.badge.isNotBlank()) {
                Box(Modifier.matchParentSize().padding(8.dp), contentAlignment = Alignment.TopStart) {
                    BadgePill(song.badge, song.badgeColor, solid = true)
                }
            }
            Box(Modifier.matchParentSize().padding(6.dp), contentAlignment = Alignment.BottomStart) {
                MiniButton(SbIcons.Heart, if (song.favorite) c.ac else Color.White, song.favorite, onFavorite)
            }
            Box(Modifier.matchParentSize().padding(6.dp), contentAlignment = Alignment.BottomEnd) {
                MiniButton(SbIcons.More, Color.White, false, onMore)
            }
        }
        Text(
            song.title,
            style = sbStyle(if (compact) 13 else 14, FontWeight.SemiBold, c.tx),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniButton(icon: com.songbook.app.ui.components.IconSpec, tint: Color, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { StrokeIcon(icon, tint, size = 18.dp, filled = filled) }
}
