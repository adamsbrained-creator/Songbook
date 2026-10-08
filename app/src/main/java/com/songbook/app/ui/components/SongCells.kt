package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/**
 * Square song tile for the grid layouts (2, 3 or 4 columns). The heart and the menu sit on top of the
 * artwork; in the 4 column layout they are smaller so the tile stays readable.
 */
@Composable
fun SongGridCell(
    song: SongEntity,
    subtitle: String,
    columns: Int,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Sb.colors
    val small = columns >= 4
    val compact = columns >= 3
    Column(modifier.clickable(onClick = onClick)) {
        Artwork(
            song.gradient,
            RoundedCornerShape(if (small) 14.dp else if (compact) 18.dp else 22.dp),
            Modifier.fillMaxWidth().aspectRatio(1f),
            image = song.image
        ) {
            if (song.badge.isNotBlank() && !small) {
                Box(Modifier.matchParentSize().padding(8.dp), contentAlignment = Alignment.TopStart) {
                    BadgePill(song.badge, song.badgeColor, solid = true)
                }
            }
            val pad = if (small) 3.dp else 6.dp
            Box(Modifier.matchParentSize().padding(pad), contentAlignment = Alignment.BottomStart) {
                MiniButton(SbIcons.Heart, if (song.favorite) c.ac else Color.White, song.favorite, onFavorite, small)
            }
            Box(Modifier.matchParentSize().padding(pad), contentAlignment = Alignment.BottomEnd) {
                MiniButton(SbIcons.More, Color.White, false, onMore, small)
            }
        }
        Text(
            song.title,
            style = sbStyle(if (small) 11 else if (compact) 13 else 14, FontWeight.SemiBold, c.tx),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (small) 5.dp else 8.dp)
        )
        if (!small) {
            Text(subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
            RatingTag(song.rating)
        }
    }
}

@Composable
fun MiniButton(icon: IconSpec, tint: Color, filled: Boolean, onClick: () -> Unit, small: Boolean = false) {
    Box(
        Modifier.size(if (small) 24.dp else 32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.30f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { StrokeIcon(icon, tint, size = if (small) 14.dp else 18.dp, filled = filled) }
}
