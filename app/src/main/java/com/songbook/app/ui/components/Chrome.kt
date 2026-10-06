package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songbook.app.data.SongEntity
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle

// ---- GLASS: floating translucent tab bar + round plus ---------------------------------------

@Composable
fun GlassTabBar(current: String?, onNavigate: (String) -> Unit, onNew: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sb.colors
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(58.dp)
                .glassSurface(RoundedCornerShape(29.dp), strong = true)
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopDests.forEach { d ->
                val on = d.route == current
                val tint = if (on) c.ac else c.mu
                Column(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (on) (if (c.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f)) else Color.Transparent)
                        .clickable { onNavigate(d.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StrokeIcon(d.icon, tint, size = 22.dp)
                    Text(d.label, style = sbStyle(9, FontWeight.Medium, tint), maxLines = 1)
                }
            }
        }
        Box(
            Modifier
                .size(58.dp)
                .shadow(12.dp, CircleShape, ambientColor = c.ac.copy(alpha = 0.5f), spotColor = c.ac.copy(alpha = 0.5f))
                .background(c.ac, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onNew)
                .semantics { contentDescription = "New song" },
            contentAlignment = Alignment.Center
        ) { StrokeIcon(SbIcons.Plus, Color.White, size = 28.dp) }
    }
}

// ---- NEW: music circle, "last song" pill, search circle -----------------------------------------

@Composable
fun MomentsBar(
    last: SongEntity?,
    lastSubtitle: String,
    onHome: () -> Unit,
    onOpenLast: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Sb.colors
    val surface = if (c.isDark) Color(0xFF26262A) else Color.White
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(50.dp).shadow(if (c.isDark) 0.dp else 4.dp, CircleShape).background(surface, CircleShape).clip(CircleShape)
                .clickable(onClick = onHome).semantics { contentDescription = "Songs" },
            contentAlignment = Alignment.Center
        ) { StrokeIcon(SbIcons.Music, c.tx, size = 22.dp) }

        Row(
            Modifier
                .weight(1f)
                .height(50.dp)
                .shadow(if (c.isDark) 0.dp else 4.dp, RoundedCornerShape(25.dp))
                .background(surface, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .clickable(enabled = last != null, onClick = onOpenLast)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (last != null) {
                SquareArtwork(last.gradient, 34.dp, 9.dp, image = last.image)
                Column(Modifier.weight(1f)) {
                    Text(last.title, style = sbStyle(12, FontWeight.Bold, c.tx), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(lastSubtitle, style = sbStyle(10, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StrokeIcon(SbIcons.Play, c.tx, size = 18.dp, filled = true)
            } else {
                Text("Nothing opened yet", style = sbStyle(12, color = c.mu))
            }
        }

        Box(
            Modifier.size(50.dp).background(c.ac.copy(alpha = if (c.isDark) 0.22f else 0.14f), CircleShape).clip(CircleShape)
                .clickable(onClick = onSearch).semantics { contentDescription = "Search" },
            contentAlignment = Alignment.Center
        ) { StrokeIcon(SbIcons.Search, c.ac, size = 22.dp) }
    }
}

/** "Songs / Albums / Favorites" pills at the top of the New look. */
@Composable
fun TopChips(current: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TopDests.filter { it.route != "settings" }.forEach { d ->
            ChipPill(d.label, active = d.route == current, onClick = { onNavigate(d.route) }, modifier = Modifier.height(36.dp))
        }
    }
}

/** Header of the New look: bold title with an arrow, optional gear, and a big accent plus. */
@Composable
fun MomentsHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    onPlus: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val c = Sb.colors
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp).height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = sbStyle(18, FontWeight.Bold, c.tx))
            StrokeIcon(SbIcons.ChevronRight, c.tx, size = 18.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing()
            if (onSettings != null) SbIconButton(SbIcons.Sliders, "Settings", onSettings, tint = c.mu, size = 20.dp)
            if (onPlus != null) SbIconButton(SbIcons.Plus, "New song", onPlus, tint = c.ac, size = 28.dp)
        }
    }
}

/** Round translucent button used by the glass look (back, more, close...). */
@Composable
fun GlassRoundButton(icon: IconSpec, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Sb.colors.tx) {
    Box(
        modifier.size(40.dp).glassSurface(CircleShape).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { StrokeIcon(icon, tint, size = 20.dp) }
}

/** Title block of the glass look: big title with a muted line under it. */
@Composable
fun GlassTitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    val c = Sb.colors
    Column(modifier) {
        Text(title, style = sbStyle(26, FontWeight.Bold, c.tx))
        if (subtitle != null) Text(subtitle, style = sbStyle(12, color = c.mu), modifier = Modifier.padding(top = 3.dp))
    }
}

/** Small spacer-width helper so call sites read well. */
@Composable
fun HSpace(w: Int) = Box(Modifier.width(w.dp))
