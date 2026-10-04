package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.kindLabel
import com.songbook.app.ui.components.BadgePill
import com.songbook.app.ui.components.ConfirmDialog
import com.songbook.app.ui.components.EditCoverDialog
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.SbIconButton
import com.songbook.app.ui.components.SbIcons
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.SquareArtwork
import com.songbook.app.ui.components.sbCard
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

    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
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
            onDismiss = { editing = false }
        ) { r ->
            onUpdateAlbum(
                album.copy(
                    name = r.name,
                    year = r.year ?: album.year,
                    gradient = r.gradient,
                    image = r.image,
                    badge = r.badge,
                    badgeColor = r.badgeColor
                )
            )
            editing = false
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete album?",
            "“${album.name}” will be removed. Its songs stay in your library.",
            "Delete",
            onConfirm = { confirmDelete = false; onDeleteAlbum(album.id) },
            onDismiss = { confirmDelete = false }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SbIconButton(SbIcons.Back, "Back", onBack, size = 24.dp)
                Text("Album", style = sbStyle(16, FontWeight.SemiBold, c.ac))
                Box {
                    SbIconButton(SbIcons.More, "More", { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(c.cd)) {
                        DropdownMenuItem(text = { Text("Edit album", style = sbStyle(14, color = c.tx)) }, onClick = { menuOpen = false; editing = true })
                        DropdownMenuItem(text = { Text("Delete album", style = sbStyle(14, color = c.ac)) }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SquareArtwork(album.gradient, 195.dp, 40.dp, elevated = true, image = album.image)
                Text(album.name, style = sbStyle(28, FontWeight.Bold, c.tx), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp))
                val meta = listOfNotNull(userName.trim().ifEmpty { null }, album.year.toString(), songCountLabel(tracks.size)).joinToString(" · ")
                Text(meta, style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
                if (album.badge.isNotBlank()) BadgePill(album.badge, album.badgeColor, Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.cd).clickable { editing = true }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Edit Album", style = sbStyle(13, FontWeight.SemiBold, c.tx)) }
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.ac).clickable { onAddSong(album.id) }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Add Song", style = sbStyle(13, FontWeight.SemiBold, Color.White)) }
                }
            }
        }
        if (tracks.isEmpty()) {
            item { EmptyHint("This album is empty. Tap “Add Song” to write the first track.") }
        } else {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp).fillMaxWidth().sbCard(28.dp).clip(RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) {
                    tracks.forEachIndexed { i, s ->
                        SongRow(
                            title = s.title,
                            subtitle = s.kindLabel(),
                            gradient = s.gradient,
                            favorite = s.favorite,
                            onClick = { onOpenSong(s.id) },
                            onFavorite = { onToggleFavorite(s) },
                            onMore = { onMenu(s.id) },
                            modifier = Modifier.padding(start = 0.dp),
                            badge = s.badge,
                            badgeColor = s.badgeColor,
                            divider = i < tracks.lastIndex,
                            leading = {
                                Text(
                                    "%02d".format(i + 1),
                                    style = sbStyle(14, color = c.mu, mono = true),
                                    modifier = Modifier.width(30.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
