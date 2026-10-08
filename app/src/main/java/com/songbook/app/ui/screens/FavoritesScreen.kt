package com.songbook.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songbook.app.data.AlbumEntity
import com.songbook.app.data.SongEntity
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.components.EmptyHint
import com.songbook.app.ui.components.GlassTitle
import com.songbook.app.ui.components.MomentsHeader
import com.songbook.app.ui.components.ScreenTitle
import com.songbook.app.ui.components.SectionTitle
import com.songbook.app.ui.components.SongRow
import com.songbook.app.ui.components.TopChips
import com.songbook.app.ui.components.sbCard
import com.songbook.app.ui.components.screenBg
import com.songbook.app.ui.components.songType
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

@Composable
fun FavoritesScreen(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    onOpenSong: (Long) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onMenu: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    onSettings: () -> Unit,
    onNew: () -> Unit,
    /** 0 = list, 2 / 3 = tiles (Settings ▸ Layout). */
    layout: Int = 0
) {
    val c = Sb.colors
    val style = Sb.style
    val favs = remember(songs) { songs.filter { it.favorite } }
    val others = remember(songs) { songs.filter { !it.favorite }.take(5) }
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }
    val cards = style != UiStyle.OLD

    @Composable
    fun FavItem(s: SongEntity, last: Boolean, fav: Boolean) = SongRow(
        title = s.title,
        subtitle = songType(s.typeName) + " · " + (albumNames[s.albumId] ?: "No album"),
        gradient = s.gradient, favorite = fav,
        onClick = { onOpenSong(s.id) }, onFavorite = { onToggleFavorite(s) }, onMore = { onMenu(s.id) },
        image = s.image, badge = s.badge, badgeColor = s.badgeColor, rating = s.rating,
        divider = !last, card = cards
    )

    LazyColumn(Modifier.fillMaxSize().screenBg().statusBarsPadding(), contentPadding = PaddingValues(bottom = 130.dp)) {
        // ---- header ----
        item {
            when (style) {
                UiStyle.NEW -> Column {
                    MomentsHeader("Favorites", onSettings = onSettings, onPlus = onNew)
                    TopChips("favorites", onNavigate)
                }
                UiStyle.GLASS -> GlassTitle("Favorites", "Songs you keep coming back to.", Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
                else -> ScreenTitle("Favorites", "Songs you keep coming back to.")
            }
        }
        // ---- the number ----
        item {
            val label = if (favs.size == 1) "favorite song" else "favorite songs"
            if (style == UiStyle.NEW) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp))
                        .background(Brush.linearGradient(listOf(c.ac, c.ac.copy(red = c.ac.red * 0.45f, green = c.ac.green * 0.35f, blue = c.ac.blue * 0.4f))))
                        .padding(24.dp)
                ) {
                    Text("${favs.size}", style = sbStyle(76, FontWeight.Bold, Color.White))
                    Text(label, style = sbStyle(26, FontWeight.Bold, Color.White))
                    Text("Songs you keep coming back to.", style = sbStyle(13, color = Color.White.copy(alpha = 0.8f)), modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth().height(76.dp).sbCard(28.dp).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${favs.size}", style = sbStyle(40, FontWeight.Bold, c.ac))
                    Text(label, style = sbStyle(16, FontWeight.Medium, c.tx))
                }
            }
        }
        if (favs.isEmpty()) {
            item { EmptyHint("Tap the heart on any song and it will show up here.") }
        } else if (layout >= 2) {
            item { Box(Modifier.height(10.dp)) }
            favs.chunked(layout).forEach { row ->
                item(key = "fr" + row.first().id) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { s ->
                            com.songbook.app.ui.components.SongGridCell(
                                s, albumNames[s.albumId] ?: "No album", layout,
                                { onOpenSong(s.id) }, { onToggleFavorite(s) }, { onMenu(s.id) }, Modifier.weight(1f)
                            )
                        }
                        repeat(layout - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        } else if (cards) {
            item { Box(Modifier.height(6.dp)) }
            favs.forEachIndexed { i, s -> item(key = s.id) { FavItem(s, i == favs.lastIndex, true) } }
        } else {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().sbCard(28.dp).clip(RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) {
                    favs.forEachIndexed { i, s -> FavItem(s, i == favs.lastIndex, true) }
                }
            }
        }
        if (style == UiStyle.NEW && others.isNotEmpty()) {
            item { SectionTitle("More songs", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)) }
            others.forEachIndexed { i, s -> item(key = "o${s.id}") { FavItem(s, i == others.lastIndex, false) } }
        }
    }
}
