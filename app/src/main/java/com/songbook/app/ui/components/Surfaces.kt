package com.songbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songbook.app.data.UiStyle
import com.songbook.app.ui.theme.Sb

/**
 * Translucent "glass" surface: a faint vertical white wash, a hairline border and, in light mode,
 * a soft shadow. Looks right on top of [GlassBackdrop].
 */
@Composable
fun Modifier.glassSurface(shape: Shape, strong: Boolean = false): Modifier {
    val c = Sb.colors
    val fill = if (c.isDark) {
        val top = if (strong) 0.20f else 0.14f
        Brush.verticalGradient(listOf(Color.White.copy(alpha = top), Color.White.copy(alpha = top / 2f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.56f)))
    }
    val border = if (c.isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.07f)
    val base = if (c.isDark) this else this.shadow(6.dp, shape, ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))
    // bars that float over scrolling content get a mostly solid base, so the text behind them cannot show through
    val solid = if (strong) base.background(c.bg.copy(alpha = if (c.isDark) 0.92f else 0.88f), shape) else base
    return solid.background(fill, shape).border(1.dp, border, shape)
}

/**
 * The card surface of every screen. Old: white card with a soft shadow. New: flat card.
 * Glass: translucent glass.
 */
@Composable
fun Modifier.sbCard(radius: Dp = 28.dp): Modifier {
    val c = Sb.colors
    val shape = RoundedCornerShape(radius)
    return when (c.style) {
        UiStyle.GLASS -> this.glassSurface(shape)
        UiStyle.NEW -> if (c.isDark) this.background(c.cd, shape) else this
            .shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.06f))
            .background(c.cd, shape)
        else -> this
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.10f))
            .background(c.cd, shape)
    }
}

/** Page background. The glass look draws its backdrop once in the root, so screens stay transparent there. */
@Composable
fun Modifier.screenBg(): Modifier = if (Sb.style == UiStyle.GLASS) this else this.background(Sb.colors.bg)

/** Dark or light base with two soft color glows, the background of the glass look. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val c = Sb.colors
    val cyan = Color(0xFF00BEDC).copy(alpha = if (c.isDark) 0.30f else 0.22f)
    val glow = c.ac.copy(alpha = if (c.isDark) 0.22f else 0.14f)
    Box(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(cyan, Color.Transparent),
                        center = Offset(size.width * 0.05f, 0f),
                        radius = size.width * 1.15f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(glow, Color.Transparent),
                        center = Offset(size.width, size.height * 0.75f),
                        radius = size.width * 1.0f
                    )
                )
            }
    )
}
