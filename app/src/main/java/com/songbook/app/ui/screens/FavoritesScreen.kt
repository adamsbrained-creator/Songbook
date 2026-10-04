package com.songbook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.ScreenTitle
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

@Composable
fun FavoritesScreen(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    onOpenSong: (Long) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onMenu: (Long) -> Unit
) {
    val c = Sb.colors
    val favs = remember(songs) { songs.filter { it.favorite } }
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item { ScreenTitle("Favorites", "Songs you keep coming back to.") }
        item {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth().height(76.dp).sbCard(28.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("${favs.size}", style = sbStyle(40, FontWeight.Bold, c.ac))
                Text(if (favs.size == 1) "favorite song" else "favorite songs", style = sbStyle(16, FontWeight.Medium, c.tx))
            }
        }
        if (favs.isEmpty()) {
            item { EmptyHint("Tap the heart on any song and it will show up here.") }
        } else {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().sbCard(28.dp).clip(RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) {
                    favs.forEach { s ->
                        SongRow(
                            title = s.title,
                            subtitle = albumNames[s.albumId] ?: "No album",
                            gradient = s.gradient,
                            favorite = true,
                            onClick = { onOpenSong(s.id) },
                            onFavorite = { onToggleFavorite(s) },
                            onMore = { onMenu(s.id) }
                        )
                    }
                }
            }
        }
    }
}
