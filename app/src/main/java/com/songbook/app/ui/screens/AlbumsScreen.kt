package com.songbook.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.components.Artwork
import com.songbook.app.ui.components.ChipPill
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.TextInputDialog
import com.songbook.app.ui.components.initials
import com.songbook.app.ui.components.songCountLabel
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import androidx.compose.foundation.shape.RoundedCornerShape as Rounded

@Composable
fun AlbumsScreen(
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    onOpenAlbum: (Long) -> Unit,
    onCreateAlbum: (String) -> Unit
) {
    val c = Sb.colors
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        TextInputDialog("New album", "Album name", confirmText = "Create",
            onConfirm = { onCreateAlbum(it); creating = false }, onDismiss = { creating = false })
    }
    val counts = remember(songs) { songs.groupingBy { it.albumId }.eachCount() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Albums", style = sbStyle(26, FontWeight.Bold, c.tx))
                    Text("Your music, organized your way.", style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
                }
                ChipPill("+ New", active = false, onClick = { creating = true })
            }
        }
        if (albums.isEmpty()) {
            item(span = { GridItemSpan(2) }) { EmptyHint("No albums yet. Tap “+ New” to create one.") }
        }
        items(albums, key = { it.id }) { a ->
            Column(Modifier.clickable { onOpenAlbum(a.id) }) {
                Artwork(a.gradient, Rounded(28.dp), Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Box(Modifier.matchParentSizeCompat().padding(start = 16.dp, bottom = 12.dp), contentAlignment = Alignment.BottomStart) {
                        Text(initials(a.name), style = sbStyle(32, FontWeight.Bold, Color.White))
                    }
                }
                Text(a.name, style = sbStyle(15, FontWeight.SemiBold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text(songCountLabel(counts[a.id] ?: 0), style = sbStyle(12, color = c.mu))
            }
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxSize()
