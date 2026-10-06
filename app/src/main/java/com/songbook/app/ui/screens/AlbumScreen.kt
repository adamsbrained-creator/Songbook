package com.songbook.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.UiStyle
import com.songbook.app.data.kindLabel
import com.songbook.app.ui.components.AlbumTypes
import com.songbook.app.ui.components.ConfirmDialog
import com.songbook.app.ui.components.CoverHeader
import com.songbook.app.ui.components.EditCoverDialog
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.HeaderMenuItem
import com.songbook.app.ui.components.RatingDialog
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.albumType
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

@Composable
fun AlbumScreen(
    album: AlbumEntity?,
    songs: List<SongEntity>,
    userName: String,
    onBack: () -> Unit,
    onOpenSong: (Long) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onMenu: (Long) -> Unit,
    onAddSong: (Long) -> Unit,
    onUpdateAlbum: (AlbumEntity) -> Unit,
    onDeleteAlbum: (Long) -> Unit
) {
    val c = Sb.colors
    // album deleted (or not found): leave the screen
    LaunchedEffect(album == null) { if (album == null) onBack() }
    if (album == null) return

    var editing by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val tracks = remember(songs, album.id) { songs.filter { it.albumId == album.id }.sortedBy { it.id } }

    if (editing) {
        EditCoverDialog(
            title = "Edit album",
            name0 = album.name,
            year0 = album.year,
            gradient0 = album.gradient,
            image0 = album.image,
            badge0 = album.badge,
            badgeColor0 = album.badgeColor,
            rating0 = album.rating,
            typeName0 = album.typeName,
            typePresets = AlbumTypes,
            typeDefault = "Album",
            onDismiss = { editing = false }
        ) { r ->
            onUpdateAlbum(
                album.copy(
                    name = r.name, year = r.year ?: album.year, gradient = r.gradient, image = r.image,
                    badge = r.badge, badgeColor = r.badgeColor, rating = r.rating, typeName = r.typeName
                )
            )
            editing = false
        }
    }
    if (rating) RatingDialog("Rate album", album.rating, { onUpdateAlbum(album.copy(rating = it)); rating = false }, { rating = false })
    if (confirmDelete) {
        ConfirmDialog(
            "Delete album?",
            "“${album.name}” will be removed. Its songs stay in your library.",
            "Delete",
            onConfirm = { confirmDelete = false; onDeleteAlbum(album.id) },
            onDismiss = { confirmDelete = false }
        )
    }

    val cardRows = Sb.style != UiStyle.OLD
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
                onPrimary = { onAddSong(album.id) },
                secondaryLabel = "Edit Album",
                onSecondary = { editing = true },
                onBack = onBack,
                menu = listOf(
                    HeaderMenuItem("Edit album") { editing = true },
                    HeaderMenuItem("Rate") { rating = true },
                    HeaderMenuItem("Delete album", danger = true) { confirmDelete = true }
                ),
                onRate = { rating = true },
                backLabel = "Albums"
            )
        }
        if (tracks.isEmpty()) {
            item { EmptyHint("This album is empty. Tap “Add Song” to write the first track.") }
        } else if (cardRows) {
            item { androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp)) }
            tracks.forEachIndexed { i, s ->
                item(key = s.id) {
                    SongRow(
                        title = s.title, subtitle = s.kindLabel(), gradient = s.gradient, favorite = s.favorite,
                        onClick = { onOpenSong(s.id) }, onFavorite = { onToggleFavorite(s) }, onMore = { onMenu(s.id) },
                        badge = s.badge, badgeColor = s.badgeColor, rating = s.rating, card = true,
                        leading = { Text("%02d".format(i + 1), style = sbStyle(14, color = c.mu, mono = true), modifier = Modifier.width(26.dp)) }
                    )
                }
            }
        } else {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp).fillMaxWidth().sbCard(28.dp).clip(RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) {
                    tracks.forEachIndexed { i, s ->
                        SongRow(
                            title = s.title, subtitle = s.kindLabel(), gradient = s.gradient, favorite = s.favorite,
                            onClick = { onOpenSong(s.id) }, onFavorite = { onToggleFavorite(s) }, onMore = { onMenu(s.id) },
                            badge = s.badge, badgeColor = s.badgeColor, rating = s.rating, divider = i < tracks.lastIndex,
                            leading = { Text("%02d".format(i + 1), style = sbStyle(14, color = c.mu, mono = true), modifier = Modifier.width(30.dp)) }
                        )
                    }
                }
            }
        }
    }
}
