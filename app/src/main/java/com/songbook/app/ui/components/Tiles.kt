package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.em
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

/**
 * Square tile used for albums and groups in the 2 / 3 column grids.
 * [selected] = null means "not in selection mode"; true / false shows the round check marker.
 * [label] is a small tag in the top-right corner (e.g. "Part II").
 */
@Composable
fun ArtTile(
    gradient: Int,
    image: String?,
    title: String,
    subtitle: String,
    initialsText: String,
    badge: String,
    badgeColor: Int,
    columns: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    selected: Boolean? = null
) {
    val c = Sb.colors
    val radius = if (columns >= 3) 22.dp else 28.dp
    val shape = RoundedCornerShape(radius)
    Column(modifier.clickable(onClick = onClick)) {
        Artwork(
            gradient, shape,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .then(if (selected == true) Modifier.border(3.dp, c.ac, shape) else Modifier),
            image = image
        ) {
            if (image == null) {
                Box(Modifier.fillMaxSize().padding(start = 14.dp, bottom = 10.dp), contentAlignment = Alignment.BottomStart) {
                    Text(initialsText, style = sbStyle(if (columns >= 3) 24 else 32, FontWeight.Bold, Color.White))
                }
            }
            if (badge.isNotBlank()) {
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) {
                    BadgePill(badge, badgeColor, solid = true)
                }
            }
            if (selected != null) {
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopEnd) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (selected) c.ac else Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) StrokeIcon(SbIcons.Check, Color.White, size = 16.dp)
                    }
                }
            } else if (label != null) {
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopEnd) {
                    Text(
                        label,
                        style = sbStyle(10, FontWeight.SemiBold, Color.White).copy(letterSpacing = 0.04.em),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.32f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text(
            title,
            style = sbStyle(if (columns >= 3) 13 else 15, FontWeight.SemiBold, c.tx),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
