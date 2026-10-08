package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

class HeaderMenuItem(val label: String, val danger: Boolean = false, val onClick: () -> Unit)

/**
 * Top of the Album and Group pages. Old: centered square artwork. New: wide hero card with the title on
 * top of the picture. Glass: centered artwork between round glass buttons.
 */
@Composable
fun CoverHeader(
    kindLabel: String,
    title: String,
    meta: String,
    gradient: Int,
    image: String?,
    badge: String,
    badgeColor: Int,
    rating: Float,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    onBack: () -> Unit,
    menu: List<HeaderMenuItem>,
    onRate: () -> Unit,
    backLabel: String = "Back",
    /** Album covers to put together when the group has no picture of its own. */
    collage: List<String> = emptyList()
) {
    val c = Sb.colors
    var open by remember { mutableStateOf(false) }

    @Composable
    fun MenuButton(glass: Boolean) {
        Box {
            if (glass) GlassRoundButton(SbIcons.More, "More", { open = true }) else SbIconButton(SbIcons.More, "More", { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(c.sheet)) {
                menu.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.label, style = sbStyle(14, color = if (m.danger) c.ac else c.tx)) },
                        onClick = { open = false; m.onClick() }
                    )
                }
            }
        }
    }

    @Composable
    fun Stars(modifier: Modifier = Modifier) {
        Box(modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onRate).padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StarRow(rating, starSize = 16.dp, gap = 3.dp)
                    Text(ratingText(rating), style = sbStyle(13, FontWeight.SemiBold, c.tx))
                }
            } else Text("Rate", style = sbStyle(13, FontWeight.Medium, c.mu))
        }
    }

    when (Sb.style) {
        UiStyle.NEW -> Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onBack).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StrokeIcon(SbIcons.Back, c.ac, size = 22.dp)
                    Text(backLabel, style = sbStyle(15, FontWeight.SemiBold, c.ac))
                }
                MenuButton(glass = false)
            }
            Artwork(
                gradient, RoundedCornerShape(30.dp),
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(250.dp),
                image = image, imagePx = 900, collage = collage
            ) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)), startY = 250f)))
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgePill(badge, badgeColor, solid = true)
                        Text(kindLabel.uppercase(), style = sbStyle(10, FontWeight.Bold, Color.White.copy(alpha = 0.85f)))
                    }
                    Text(title, style = sbStyle(34, FontWeight.Bold, Color.White), maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                    Text(meta, style = sbStyle(12, color = Color.White.copy(alpha = 0.85f)))
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(c.ac).clickable(onClick = onPrimary),
                    contentAlignment = Alignment.Center
                ) { Text(primaryLabel, style = sbStyle(15, FontWeight.Bold, Color.White)) }
                Box(
                    Modifier.height(50.dp).clip(RoundedCornerShape(25.dp)).background(c.cd).clickable(onClick = onSecondary).padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) { Text(secondaryLabel, style = sbStyle(14, FontWeight.SemiBold, c.tx)) }
            }
            Stars(Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }

        UiStyle.GLASS -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassRoundButton(SbIcons.Back, "Back", onBack)
                Text(kindLabel, style = sbStyle(15, FontWeight.SemiBold, c.ac))
                MenuButton(glass = true)
            }
            SquareArtwork(gradient, 150.dp, 34.dp, elevated = true, image = image, collage = collage)
            Text(title, style = sbStyle(28, FontWeight.Bold, c.tx), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp))
            Text(meta, style = sbStyle(13, color = c.mu), modifier = Modifier.padding(top = 2.dp))
            BadgePill(badge, badgeColor, Modifier.padding(top = 8.dp))
            Stars()
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.height(42.dp).glassSurface(RoundedCornerShape(21.dp)).clip(RoundedCornerShape(21.dp)).clickable(onClick = onSecondary).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(secondaryLabel, style = sbStyle(13, FontWeight.SemiBold, c.tx)) }
                Box(
                    Modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(c.ac).clickable(onClick = onPrimary).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(primaryLabel, style = sbStyle(13, FontWeight.SemiBold, Color.White)) }
            }
        }

        else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SbIconButton(SbIcons.Back, "Back", onBack, size = 24.dp)
                Text(kindLabel, style = sbStyle(16, FontWeight.SemiBold, c.ac))
                MenuButton(glass = false)
            }
            Spacer8()
            SquareArtwork(gradient, 195.dp, 40.dp, elevated = true, image = image, collage = collage)
            Text(title, style = sbStyle(28, FontWeight.Bold, c.tx), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp))
            Text(meta, style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
            BadgePill(badge, badgeColor, Modifier.padding(top = 8.dp))
            Stars()
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.cd).clickable(onClick = onSecondary).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(secondaryLabel, style = sbStyle(13, FontWeight.SemiBold, c.tx)) }
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(c.ac).clickable(onClick = onPrimary).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(primaryLabel, style = sbStyle(13, FontWeight.SemiBold, Color.White)) }
            }
        }
    }
}

@Composable
private fun Spacer8() = Box(Modifier.height(8.dp))
