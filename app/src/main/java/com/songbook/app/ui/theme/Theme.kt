package com.songbook.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.songbook.app.R
import com.songbook.app.data.UiStyle

/** Values copied from the mockups: --bg, --tx, --mu, --ac, --cd, --ln, plus a few extras for the new looks. */
@Immutable
class SbColors(
    val bg: Color,
    val tx: Color,
    val mu: Color,
    val ac: Color,
    val cd: Color,
    val ln: Color,
    val isDark: Boolean,
    /** Opaque surface for dialogs and sheets (the glass look keeps [cd] for solid fallbacks only). */
    val sheet: Color = cd,
    /** Track behind segmented controls and inset fields. */
    val track: Color = bg,
    /** [UiStyle.OLD], [UiStyle.NEW] or [UiStyle.GLASS]. */
    val style: Int = UiStyle.OLD
)

val AccentLight = listOf(Color(0xFFE5173F), Color(0xFF3A63B8), Color(0xFFC2610C))
val AccentDark = listOf(Color(0xFFFF3B62), Color(0xFF6C93F0), Color(0xFFE8812A))

/**
 * Accent for the three presets (index 0..2) or the user's own RGB color (index 3). Custom colors are
 * lifted a little in dark mode and darkened when they are so bright that white text would be unreadable.
 */
fun accentColor(dark: Boolean, accent: Int, custom: Int): Color {
    if (accent in 0..2) return if (dark) AccentDark[accent] else AccentLight[accent]
    var c = Color(custom).copy(alpha = 1f)
    if (dark) c = lerp(c, Color.White, 0.10f)
    repeat(6) { if (c.luminance() > 0.5f) c = lerp(c, Color.Black, 0.12f) }
    return c
}

fun sbColors(dark: Boolean, accent: Int, custom: Int = 0, style: Int = UiStyle.OLD): SbColors {
    val ac = accentColor(dark, accent, custom)
    return when (style) {
        UiStyle.NEW -> if (dark) {
            SbColors(
                bg = Color(0xFF000000), tx = Color.White, mu = Color(0xFF98989F), ac = ac,
                cd = Color(0xFF1D1D20), ln = Color(0xFF2C2C30), isDark = true,
                sheet = Color(0xFF1D1D20), track = Color(0xFF000000), style = style
            )
        } else {
            SbColors(
                bg = Color(0xFFF2F2F7), tx = Color(0xFF111114), mu = Color(0xFF6C6C72), ac = ac,
                cd = Color.White, ln = Color(0xFFE5E5EA), isDark = false,
                sheet = Color.White, track = Color(0xFFF2F2F7), style = style
            )
        }
        UiStyle.GLASS -> if (dark) {
            SbColors(
                bg = Color(0xFF0A0B0F), tx = Color.White, mu = Color(0xFF9A9EAB), ac = ac,
                cd = Color(0xFF1B1D25), ln = Color(0x29FFFFFF), isDark = true,
                sheet = Color(0xF21E2028), track = Color(0x1AFFFFFF), style = style
            )
        } else {
            SbColors(
                bg = Color(0xFFF2F3F7), tx = Color(0xFF111114), mu = Color(0xFF7D818D), ac = ac,
                cd = Color.White, ln = Color(0x14000000), isDark = false,
                sheet = Color(0xF7FFFFFF), track = Color(0x0F000000), style = style
            )
        }
        else -> if (dark) {
            SbColors(
                bg = Color(0xFF0B0B0D), tx = Color.White, mu = Color(0xFF9A9AA2), ac = ac,
                cd = Color(0xFF1C1C1F), ln = Color(0xFF2A2A2E), isDark = true, style = style
            )
        } else {
            SbColors(
                bg = Color(0xFFF5F5F7), tx = Color(0xFF111114), mu = Color(0xFF6B6B72), ac = ac,
                cd = Color.White, ln = Color(0xFFE8E8ED), isDark = false, style = style
            )
        }
    }
}

val LocalSb = staticCompositionLocalOf { sbColors(false, 0) }

object Sb {
    val colors: SbColors
        @Composable get() = LocalSb.current

    /** [UiStyle.OLD], [UiStyle.NEW] or [UiStyle.GLASS] */
    val style: Int
        @Composable get() = LocalSb.current.style
}

@Composable
fun SongbookTheme(dark: Boolean, accent: Int, accentCustom: Int, style: Int, content: @Composable () -> Unit) {
    val c = remember(dark, accent, accentCustom, style) { sbColors(dark, accent, accentCustom, style) }
    val scheme = if (dark) {
        darkColorScheme(primary = c.ac, background = c.bg, surface = c.sheet, onSurface = c.tx, onBackground = c.tx)
    } else {
        lightColorScheme(primary = c.ac, background = c.bg, surface = c.sheet, onSurface = c.tx, onBackground = c.tx)
    }
    CompositionLocalProvider(LocalSb provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

// ---- typography ----------------------------------------------------------------------------

/**
 * Poppins is bundled in res/font. Only Regular, Medium and Bold are included, so the mockup's
 * SemiBold (600) maps to the Bold file. Drop poppins_semibold.ttf into res/font and point the
 * SemiBold line at it for an exact match.
 */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

fun sbStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    mono: Boolean = false
) = TextStyle(
    fontFamily = if (mono) FontFamily.Monospace else Poppins,
    fontSize = size.sp,
    fontWeight = weight,
    color = color
)

// ---- artwork gradients (135deg, from the mockup) -------------------------------------------

val SongGradients: List<List<Color>> = listOf(
    listOf(Color(0xFFE5173F), Color(0xFFFF7A9A)), // red
    listOf(Color(0xFF0F8BA8), Color(0xFFF0668A)), // teal -> pink
    listOf(Color(0xFF23323F), Color(0xFF9A8CE0)), // navy -> violet
    listOf(Color(0xFFC2610C), Color(0xFF3FA66B))  // orange -> green
)

fun gradientBrush(index: Int): Brush {
    val n = SongGradients.size
    return Brush.linearGradient(SongGradients[((index % n) + n) % n])
}
