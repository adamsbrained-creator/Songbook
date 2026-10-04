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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.songbook.app.R

/** Values copied from the mockup: --bg, --tx, --mu, --ac, --cd, --ln */
@Immutable
class SbColors(
    val bg: Color,
    val tx: Color,
    val mu: Color,
    val ac: Color,
    val cd: Color,
    val ln: Color,
    val isDark: Boolean
)

val AccentLight = listOf(Color(0xFFE5173F), Color(0xFF3A63B8), Color(0xFFC2610C))
val AccentDark = listOf(Color(0xFFFF3B62), Color(0xFF6C93F0), Color(0xFFE8812A))

fun sbColors(dark: Boolean, accent: Int): SbColors {
    val i = accent.coerceIn(0, 2)
    return if (dark) {
        SbColors(
            bg = Color(0xFF0B0B0D), tx = Color.White, mu = Color(0xFF9A9AA2), ac = AccentDark[i],
            cd = Color(0xFF1C1C1F), ln = Color(0xFF2A2A2E), isDark = true
        )
    } else {
        SbColors(
            bg = Color(0xFFF5F5F7), tx = Color(0xFF111114), mu = Color(0xFF6B6B72), ac = AccentLight[i],
            cd = Color.White, ln = Color(0xFFE8E8ED), isDark = false
        )
    }
}

val LocalSb = staticCompositionLocalOf { sbColors(false, 0) }

object Sb {
    val colors: SbColors
        @Composable get() = LocalSb.current
}

@Composable
fun SongbookTheme(dark: Boolean, accent: Int, content: @Composable () -> Unit) {
    val c = remember(dark, accent) { sbColors(dark, accent) }
    val scheme = if (dark) {
        darkColorScheme(primary = c.ac, background = c.bg, surface = c.cd, onSurface = c.tx, onBackground = c.tx)
    } else {
        lightColorScheme(primary = c.ac, background = c.bg, surface = c.cd, onSurface = c.tx, onBackground = c.tx)
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
