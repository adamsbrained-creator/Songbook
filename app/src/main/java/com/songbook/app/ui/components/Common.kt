package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.songbook.app.data.UiStyle
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

/** "Demo", "Cover"... or "Original song" when no custom type was set. */
fun songType(typeName: String) = typeName.ifBlank { "Original song" }

/** "Single", "EP"... or "Album". */
fun albumType(typeName: String) = typeName.ifBlank { "Album" }

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
    /** Pictures to put together as a collage when there is no [image] (a group of albums). */
    collage: List<String> = emptyList(),
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
        } else if (image == null && collage.isNotEmpty()) {
            CollageArt(collage, imagePx)
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

/** Up to four album covers cut into one picture: 1 = whole, 2 = halves, 3 = one big and two small, 4 = 2 x 2. */
@Composable
private fun BoxScope.CollageArt(names: List<String>, px: Int) {
    val half = (px / 2).coerceAtLeast(96)
    val a = rememberCover(names.getOrNull(0), if (names.size == 1) px else half)
    val b = rememberCover(names.getOrNull(1), half)
    val c = rememberCover(names.getOrNull(2), half)
    val d = rememberCover(names.getOrNull(3), half)
    @Composable
    fun Cell(bmp: androidx.compose.ui.graphics.ImageBitmap?, m: Modifier) {
        Box(m) {
            if (bmp != null) Image(bmp, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
    }
    Box(Modifier.matchParentSize()) {
        when {
            names.size == 1 -> Cell(a, Modifier.matchParentSize())
            names.size == 2 -> Row(Modifier.matchParentSize()) {
                Cell(a, Modifier.weight(1f).fillMaxHeight())
                Cell(b, Modifier.weight(1f).fillMaxHeight())
            }
            names.size == 3 -> Row(Modifier.matchParentSize()) {
                Cell(a, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Cell(b, Modifier.weight(1f).fillMaxWidth())
                    Cell(c, Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(Modifier.matchParentSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Cell(a, Modifier.weight(1f).fillMaxHeight())
                    Cell(b, Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Cell(c, Modifier.weight(1f).fillMaxHeight())
                    Cell(d, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
fun SquareArtwork(
    gradient: Int,
    size: Dp,
    corner: Dp,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    image: String? = null,
    collage: List<String> = emptyList()
) {
    val shape = RoundedCornerShape(corner)
    val m = if (elevated) {
        modifier.size(size).shadow(10.dp, shape, ambientColor = Color.Black.copy(0.14f), spotColor = Color.Black.copy(0.2f))
    } else modifier.size(size)
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(128, 1024)
    Artwork(gradient, shape, m, image = image, imagePx = px, collage = collage)
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

/**
 * Rounded chip used in the reader header, editor toolbar and New Song sheet.
 * [inactiveBg] left unspecified = the card color (glass look: a glass pill).
 */
@Composable
fun ChipPill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inactiveBg: Color = Color.Unspecified
) {
    val c = Sb.colors
    val shape = RoundedCornerShape(20.dp)
    val glass = c.style == UiStyle.GLASS && !active && inactiveBg == Color.Unspecified
    val base = modifier.height(40.dp).clip(shape)
    val filled = when {
        active -> base.background(c.ac)
        glass -> base.glassSurface(shape)
        else -> base.background(if (inactiveBg == Color.Unspecified) c.cd else inactiveBg)
    }
    Box(
        filled.clickable(onClick = onClick).padding(horizontal = 16.dp),
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
            .background(if (checked) (if (c.style == UiStyle.OLD) c.ac else Color(0xFF34C759)) else Color(0xFFAEAEB2))
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
    val old = c.style == UiStyle.OLD
    Row(
        Modifier
            .clip(RoundedCornerShape(if (old) 12.dp else 20.dp))
            .background(c.track)
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg = if (!on) Color.Transparent else if (old) c.cd else c.ac
            val fg = if (!on) c.mu else if (old) c.ac else Color.White
            Box(
                Modifier
                    .height(if (old) 40.dp else 36.dp)
                    .clip(RoundedCornerShape(if (old) 10.dp else 18.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, style = sbStyle(12, FontWeight.SemiBold, fg)) }
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
    divider: Boolean = false,
    rating: Float = 0f,
    /** Draws the row as its own rounded card (used by the New and Glass looks). */
    card: Boolean = false,
    /** Smaller rows with a smaller cover. */
    compact: Boolean = false
) {
    val c = Sb.colors
    val art = if (compact) 34.dp else 44.dp
    val inset = if (leading != null) 60.dp else if (compact) 64.dp else 74.dp
    val outer = if (card) {
        modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .sbCard(20.dp)
            .clip(RoundedCornerShape(20.dp))
    } else modifier.fillMaxWidth()
    Row(
        outer
            .height(if (compact) 48.dp else if (card) 62.dp else 58.dp)
            .drawBehind {
                if (divider && !card) {
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(c.ln, Offset(inset.toPx(), y), Offset(size.width - 18.dp.toPx(), y), 1.dp.toPx())
                }
            }
            .clickable(onClick = onClick)
            .padding(start = if (card) 12.dp else 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) leading() else CircleArtwork(gradient, art, image = image)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    subtitle,
                    style = sbStyle(12, color = c.mu),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                RatingTag(rating)
            }
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
