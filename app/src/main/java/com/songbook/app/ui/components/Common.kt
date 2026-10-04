package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.songbook.app.data.AppSettings
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.gradientBrush
import com.songbook.app.ui.theme.sbStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---- small helpers -------------------------------------------------------------------------

fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "?"
    }
}

fun formatLongDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))

fun songCountLabel(n: Int) = if (n == 1) "1 song" else "$n songs"

fun albumCountLabel(n: Int) = if (n == 1) "1 album" else "$n albums"

/** "today", "yesterday", "5 days ago", "3 weeks ago"... for the Workflow page. */
fun relativeDay(millis: Long): String {
    val days = ((System.currentTimeMillis() - millis) / 86_400_000L).toInt().coerceAtLeast(0)
    return when {
        days == 0 -> "today"
        days == 1 -> "yesterday"
        days < 14 -> "$days days ago"
        days < 60 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}

/** What a group of [albums] albums is called: 3 albums = Trilogy. */
fun groupKind(albums: Int): String = when (albums) {
    2 -> "Duology"
    3 -> "Trilogy"
    4 -> "Tetralogy"
    5 -> "Pentalogy"
    else -> "Collection"
}

/** "Hello John", "Good evening John"... depending on the Greeting setting. */
fun greetingLine(settings: AppSettings, hour: Int = java.time.LocalTime.now().hour): String {
    val base = when (settings.greeting) {
        1 -> "Hi"
        2 -> "Hey"
        3 -> "Welcome back"
        4 -> when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..22 -> "Good evening"
            else -> "Hello"
        }
        5 -> settings.greetingCustom.trim().ifEmpty { "Hello" }
        else -> "Hello"
    }
    val name = settings.userName.trim()
    return if (name.isEmpty()) base else "$base $name"
}

// ---- surfaces ------------------------------------------------------------------------------

/** White (or dark) card with the mockup's soft shadow. */
@Composable
fun Modifier.sbCard(radius: Dp = 28.dp): Modifier {
    val c = Sb.colors
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f))
        .background(c.cd, shape)
}

/**
 * Gradient artwork tile with the translucent circle in the bottom-right corner. When [image] (a stored
 * cover file name) is set, the picture is shown instead, cropped to fill the tile.
 */
@Composable
fun Artwork(
    gradient: Int,
    shape: Shape,
    modifier: Modifier = Modifier,
    image: String? = null,
    imagePx: Int = 600,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val cover = rememberCover(image, imagePx)
    Box(modifier.clip(shape).background(gradientBrush(gradient))) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = w * 0.3f,
                    center = Offset(w * 0.85f, size.height * 0.85f)
                )
            }
        }
        content()
    }
}

@Composable
fun SquareArtwork(
    gradient: Int,
    size: Dp,
    corner: Dp,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    image: String? = null
) {
    val shape = RoundedCornerShape(corner)
    val m = if (elevated) {
        modifier.size(size).shadow(10.dp, shape, ambientColor = Color.Black.copy(0.14f), spotColor = Color.Black.copy(0.2f))
    } else modifier.size(size)
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(128, 1024)
    Artwork(gradient, shape, m, image = image, imagePx = px)
}

@Composable
fun CircleArtwork(gradient: Int, size: Dp, modifier: Modifier = Modifier, image: String? = null) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(96, 512)
    Artwork(gradient, CircleShape, modifier.size(size), image = image, imagePx = px)
}

// ---- buttons & chips -----------------------------------------------------------------------

@Composable
fun SbIconButton(
    icon: IconSpec,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Sb.colors.tx,
    size: Dp = 22.dp,
    filled: Boolean = false
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { StrokeIcon(icon, tint, size = size, filled = filled) }
}

@Composable
fun HeartButton(favorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sb.colors
    SbIconButton(
        icon = SbIcons.Heart,
        description = if (favorite) "Remove from favorites" else "Add to favorites",
        onClick = onClick,
        modifier = modifier,
        tint = if (favorite) c.ac else c.mu,
        filled = favorite
    )
}

/** Rounded chip used in the reader header, editor toolbar and New Song sheet. */
@Composable
fun ChipPill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inactiveBg: Color = Sb.colors.cd
) {
    val c = Sb.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (active) c.ac else inactiveBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = sbStyle(13, FontWeight.SemiBold, if (active) Color.White else c.tx), maxLines = 1)
    }
}

/** "All / Lyrics / Chords / Tabs" filter in the Library. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Sb.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) c.cd else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = sbStyle(13, FontWeight.Medium, if (selected) c.ac else c.mu))
    }
}

@Composable
fun SbSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Sb.colors
    val thumbX by animateDpAsState(if (checked) 23.dp else 3.dp, label = "thumb")
    Box(
        Modifier
            .size(48.dp, 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) c.ac else Color(0xFFAEAEB2))
            .clickable { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = thumbX, y = 3.dp)
                .size(22.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = Sb.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.cd else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, style = sbStyle(12, FontWeight.SemiBold, if (on) c.ac else c.mu)) }
        }
    }
}

// ---- headings & rows -----------------------------------------------------------------------

@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val c = Sb.colors
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
        Text(title, style = sbStyle(26, FontWeight.Bold, c.tx))
        Text(subtitle, style = sbStyle(14, color = c.mu), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = sbStyle(20, FontWeight.Bold, Sb.colors.ac), modifier = modifier)
}

@Composable
fun SongRow(
    title: String,
    subtitle: String,
    gradient: Int,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    image: String? = null,
    badge: String = "",
    badgeColor: Int = 0,
    /** Thin separator under the row (hide it on the last row of a list). */
    divider: Boolean = false
) {
    val c = Sb.colors
    val inset = if (leading != null) 60.dp else 74.dp
    Row(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .drawBehind {
                if (divider) {
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(c.ln, Offset(inset.toPx(), y), Offset(size.width - 18.dp.toPx(), y), 1.dp.toPx())
                }
            }
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) leading() else CircleArtwork(gradient, 44.dp, image = image)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = sbStyle(14, FontWeight.SemiBold, c.tx),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                BadgePill(badge, badgeColor)
            }
            Text(subtitle, style = sbStyle(12, color = c.mu), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HeartButton(favorite, onFavorite)
        SbIconButton(SbIcons.More, "More", onMore)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val c = Sb.colors
    Text(title, style = sbStyle(13, FontWeight.SemiBold, c.ac), modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp))
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().sbCard(24.dp).clip(RoundedCornerShape(24.dp))) { content() }
}

@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val c = Sb.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                if (!first) drawLine(c.ln, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .height(52.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = sbStyle(14, FontWeight.Medium, c.tx))
        trailing()
    }
}

@Composable
fun MutedValue(text: String) {
    Text(text, style = sbStyle(13, color = Sb.colors.mu))
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = sbStyle(14, color = Sb.colors.mu),
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp)
    )
}

// ---- bottom navigation ---------------------------------------------------------------------

class TopDest(val route: String, val label: String, val icon: IconSpec)

val TopDests = listOf(
    TopDest("library", "Songs", SbIcons.Music),
    TopDest("albums", "Albums", SbIcons.Albums),
    TopDest("favorites", "Favorites", SbIcons.Heart),
    TopDest("settings", "Settings", SbIcons.Sliders)
)

/** Height of the nav bar content, not counting the system navigation bar inset. */
val NavBarHeight = 78.dp

@Composable
fun SbBottomBar(current: String?, raised: Boolean, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Sb.colors
    val shape = if (raised) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RoundedCornerShape(0.dp)
    val base = if (raised) {
        modifier.shadow(10.dp, shape, ambientColor = Color.Black.copy(0.07f), spotColor = Color.Black.copy(0.10f))
    } else modifier
    Box(
        base
            .fillMaxWidth()
            .background(c.cd, shape)
            .then(
                if (!raised) Modifier.drawBehind {
                    drawLine(c.ln, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                } else Modifier
            )
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(NavBarHeight).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TopDests.forEachIndexed { i, d ->
                if (raised && i == 2) Spacer(Modifier.width(64.dp))
                val on = d.route == current
                val tint = if (on) c.ac else c.mu
                Column(
                    Modifier
                        .width(64.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(d.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StrokeIcon(d.icon, tint, size = 24.dp)
                    Text(d.label, style = sbStyle(10, FontWeight.Medium, tint), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun SbFab(raised: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sb.colors
    val shape: Shape = if (raised) CircleShape else RoundedCornerShape(18.dp)
    Box(
        modifier
            .size(56.dp)
            .shadow(10.dp, shape, ambientColor = c.ac.copy(alpha = 0.35f), spotColor = c.ac.copy(alpha = 0.35f))
            .background(c.ac, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "New song" },
        contentAlignment = Alignment.Center
    ) { StrokeIcon(SbIcons.Plus, Color.White, size = 26.dp) }
}
