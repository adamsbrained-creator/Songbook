package com.songbook.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The notes of the launcher icon (res/drawable/ic_launcher_foreground.xml), drawn on a 108 x 108 grid. */
private const val NotePath =
    "M49.59,70.73C51.25,74.46 48.31,79.40 43.01,81.76C37.71,84.12 32.07,83.00 30.41,79.27C28.75,75.54 31.69,70.60 36.99,68.24C42.29,65.88 47.93,67.00 49.59,70.73z M77.59,64.73C79.25,68.46 76.31,73.40 71.01,75.76C65.71,78.12 60.07,77.00 58.41,73.27C56.75,69.54 59.69,64.60 64.99,62.24C70.29,59.88 75.93,61.00 77.59,64.73z M45.00,35.00L50.00,33.70L50.00,76.00L45.00,76.00z M73.00,26.50L78.00,25.20L78.00,70.00L73.00,70.00z M45.00,35.00L78.00,26.50L78.00,35.50L45.00,44.00z M45.00,47.50L78.00,39.00L78.00,45.80L45.00,54.30z"

/**
 * In-app copy of the Songbook 2.0 icon: coral-to-crimson tile with a soft light and a darker corner,
 * three shadow layers under the notes and a white-to-pink gradient on them. The notes are drawn a bit
 * larger than in the launcher icon because the whole tile is visible here.
 */
@Composable
fun AppIconArt(size: Dp, modifier: Modifier = Modifier) {
    val note = remember { PathParser().parsePathString(NotePath).toPath() }
    val shape = RoundedCornerShape(size * 0.225f)
    Canvas(modifier.size(size).shadow(if (size > 60.dp) 14.dp else 0.dp, shape).clip(shape)) {
        val s = this.size.width / 108f
        scale(s, s, pivot = Offset.Zero) {
            val tile = Size(108f, 108f)
            drawRect(
                Brush.linearGradient(
                    0f to Color(0xFFFF6B88), 0.55f to Color(0xFFE81F4A), 1f to Color(0xFFB00D36),
                    start = Offset(0f, 0f), end = Offset(108f, 108f)
                ),
                size = tile
            )
            drawRect(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent), center = Offset(28f, 20f), radius = 78f),
                size = tile
            )
            drawRect(
                Brush.radialGradient(listOf(Color(0xFF3C0014).copy(alpha = 0.40f), Color.Transparent), center = Offset(100f, 104f), radius = 62f),
                size = tile
            )
            scale(1.22f, 1.22f, pivot = Offset(54f, 54f)) {
                for ((dy, a) in listOf(6.5f to 0.08f, 4.5f to 0.12f, 2.5f to 0.16f)) {
                    translate(0f, dy) { drawPath(note, Color(0xFF46001A).copy(alpha = a)) }
                }
                drawPath(note, Brush.verticalGradient(listOf(Color.White, Color(0xFFFFCDDA)), startY = 26f, endY = 84f))
            }
        }
    }
}
