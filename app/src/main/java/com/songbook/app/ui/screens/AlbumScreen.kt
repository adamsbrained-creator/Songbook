package com.songbook.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.AppSettings
import com.songbook.app.data.SongEntity
import com.songbook.app.data.TrackSort
import com.songbook.app.data.UiStyle
import com.songbook.app.data.kindLabel
import com.songbook.app.data.sideList
import com.songbook.app.data.sideLetter
import com.songbook.app.data.sortedTracks
import com.songbook.app.ui.components.AlbumTypes
import com.songbook.app.ui.components.ChoiceDialog
import com.songbook.app.ui.components.ConfirmDialog
import com.songbook.app.ui.components.CoverHeader
import com.songbook.app.ui.components.EditCoverDialog
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.HeaderMenuItem
import com.songbook.app.ui.components.RatingDialog
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.SidesDialog
import com.songbook.app.ui.components.SongGridCell
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.albumType
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

class AlbumActions(
    val onBack: () -> Unit,
    val onOpenSong: (Long) -> Unit,
    val onToggleFavorite: (SongEntity) -> Unit,
    val onMenu: (Long) -> Unit,
    val onAddSong: (Long) -> Unit,
    val onUpdateAlbum: (AlbumEntity) -> Unit,
    val onDeleteAlbum: (Long) -> Unit,
    val onTrackLayout: (Int) -> Unit,
    val onTrackSort: (Int) -> Unit,
    val onArrange: (Long) -> Unit,
    val onSetSides: (AlbumEntity, List<String>) -> Unit,
    val onRemoveSide: (AlbumEntity, Int) -> Unit,
    val onFollowCover: (Long) -> Unit
)

private val TrackLayouts = listOf("List", "Grid, 2 columns", "Grid, 3 columns")

@Composable
fun AlbumScreen(
    album: AlbumEntity?,
    songs: List<SongEntity>,
    settings: AppSettings,
    userName: String,
    actions: AlbumActions
) {
    val c = Sb.colors
    // album deleted (or not found): leave the screen
    LaunchedEffect(album == null) { if (album == null) actions.onBack() }
    if (album == null) return

    var editing by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var dialog by remember { mutableIntStateOf(0) }   // 1 sides, 2 layout, 3 sort, 4 use cover
    val sides = album.sideList
    val tracks = remember(songs, album.id) { songs.filter { it.albumId == album.id } }

    if (editing) {
        EditCoverDialog(
            title = "Edit album",
            name0 = album.name, year0 = album.year, gradient0 = album.gradient, image0 = album.image,
            badge0 = album.badge, badgeColor0 = album.badgeColor, rating0 = album.rating, typeName0 = album.typeName,
            typePresets = AlbumTypes, typeDefault = "Album",
            onDismiss = { editing = false }
        ) { r ->
            actions.onUpdateAlbum(
                album.copy(
                    name = r.name, year = r.year ?: album.year, gradient = r.gradient, image = r.image,
                    badge = r.badge, badgeColor = r.badgeColor, rating = r.rating, typeName = r.typeName
                )
            )
            editing = false
        }
    }
    if (rating) RatingDialog("Rate album", album.rating, { actions.onUpdateAlbum(album.copy(rating = it)); rating = false }, { rating = false })
    if (confirmDelete) {
        ConfirmDialog(
            "Delete album?", "“${album.name}” will be removed. Its songs stay in your library.", "Delete",
            onConfirm = { confirmDelete = false; actions.onDeleteAlbum(album.id) },
            onDismiss = { confirmDelete = false }
        )
    }
    if (dialog == 1) {
        SidesDialog(
            sides = sides,
            onSave = { names -> actions.onSetSides(album, names); dialog = 0 },
            onRemove = { i -> actions.onRemoveSide(album, i) },
            onDismiss = { dialog = 0 }
        )
    }
    if (dialog == 2) {
        val idx = when (settings.trackLayout) { 2 -> 1; 3 -> 2; else -> 0 }
        ChoiceDialog("Song layout", TrackLayouts, idx, { actions.onTrackLayout(if (it == 0) 0 else it + 1); dialog = 0 }, { dialog = 0 })
    }
    if (dialog == 3) ChoiceDialog("Sort songs", TrackSort.names, settings.trackSort, { actions.onTrackSort(it); dialog = 0 }, { dialog = 0 })
    if (dialog == 4) {
        ConfirmDialog(
            "Use this cover for all songs?",
            "Every song in “${album.name}” will show the album cover again, also the ones you gave their own picture.",
            "Use cover",
            onConfirm = { actions.onFollowCover(album.id); dialog = 0 },
            onDismiss = { dialog = 0 }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().screenBg().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            val meta = listOfNotNull(userName.trim().ifEmpty { null }, album.year.toString(), songCountLabel(tracks.size)).joinToString(" · ")
            CoverHeader(
                kindLabel = albumType(album.typeName),
                title = album.name,
                meta = meta,
                gradient = album.gradient,
                image = album.image,
                badge = album.badge,
                badgeColor = album.badgeColor,
                rating = album.rating,
                primaryLabel = "Add Song",
                onPrimary = { actions.onAddSong(album.id) },
                secondaryLabel = "Edit Album",
                onSecondary = { editing = true },
                onBack = actions.onBack,
                menu = listOf(
                    HeaderMenuItem("Edit album") { editing = true },
                    HeaderMenuItem("Rate") { rating = true },
                    HeaderMenuItem(if (sides.isEmpty()) "Add sides (Side A, B...)" else "Sides") { dialog = 1 },
                    HeaderMenuItem("Arrange songs") { actions.onArrange(album.id) },
                    HeaderMenuItem("Sort songs") { dialog = 3 },
                    HeaderMenuItem("Song layout") { dialog = 2 },
                    HeaderMenuItem("Use this cover for all songs") { dialog = 4 },
                    HeaderMenuItem("Delete album", danger = true) { confirmDelete = true }
                ),
                onRate = { rating = true },
                backLabel = "Albums"
            )
        }
        if (tracks.isEmpty()) {
            item { EmptyHint("This album is empty. Tap “Add Song” to write the first track.") }
        } else if (sides.isEmpty()) {
            item { Box(Modifier.height(12.dp)) }
            trackGroup(null, tracks.sortedTracks(settings.trackSort), null, settings, actions)
        } else {
            sides.forEachIndexed { i, name ->
                val onSide = tracks.filter { it.side.coerceIn(0, sides.lastIndex) == i }.sortedTracks(settings.trackSort)
                item { SectionTitle(name, Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 4.dp)) }
                if (onSide.isEmpty()) {
                    item { Text("No songs on this side yet. Use a song's menu ▸ Side.", style = sbStyle(12, color = c.mu), modifier = Modifier.padding(start = 24.dp, top = 6.dp)) }
                } else {
                    trackGroup(name, onSide, sideLetter(name), settings, actions)
                }
            }
        }
    }
}

/** The songs of one side (or the whole album) as a list or as tiles, numbered 01, 02... or A1, A2... */
private fun LazyListScope.trackGroup(
    @Suppress("UNUSED_PARAMETER") title: String?,
    list: List<SongEntity>,
    letter: String?,
    settings: AppSettings,
    a: AlbumActions
) {
    val layout = settings.trackLayout
    if (layout >= 2) {
        list.chunked(layout).forEach { row ->
            item(key = "t" + row.first().id) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s ->
                        SongGridCell(
                            s, s.kindLabel(), layout,
                            { a.onOpenSong(s.id) }, { a.onToggleFavorite(s) }, { a.onMenu(s.id) }, Modifier.weight(1f)
                        )
                    }
                    repeat(layout - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
        return
    }
    item(key = "tl" + list.first().id) {
        val c = Sb.colors
        val cards = Sb.style != UiStyle.OLD
        val rows: @Composable () -> Unit = {
            list.forEachIndexed { i, s ->
                SongRow(
                    title = s.title, subtitle = s.kindLabel(), gradient = s.gradient, favorite = s.favorite,
                    onClick = { a.onOpenSong(s.id) }, onFavorite = { a.onToggleFavorite(s) }, onMore = { a.onMenu(s.id) },
                    badge = s.badge, badgeColor = s.badgeColor, rating = s.rating,
                    divider = i < list.lastIndex, card = cards,
                    leading = {
                        Text(
                            if (letter != null) "$letter${i + 1}" else "%02d".format(i + 1),
                            style = sbStyle(14, color = c.mu, mono = true), modifier = Modifier.width(if (cards) 30.dp else 34.dp)
                        )
                    }
                )
            }
        }
        if (cards) Column { rows() }
        else Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().sbCard(28.dp).clip(RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) { rows() }
    }
}
