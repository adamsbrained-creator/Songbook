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

/** Same notes as the launcher icon (res/drawable/ic_launcher_*.xml), drawn on a 108 x 108 grid. Slightly larger, since the whole tile is visible here. */
private val NotePaths = listOf(
    "M35,71a8,8 0 1,0 16,0a8,8 0 1,0 -16,0z",   // left head
    "M61,65a8,8 0 1,0 16,0a8,8 0 1,0 -16,0z",   // right head
    "M45,35h6v36h-6z",                           // left stem
    "M71,27h6v38h-6z",                           // right stem
    "M45,35L77,27L77,38L45,46z"                  // beam
)

@Composable
fun AppIconArt(size: Dp, modifier: Modifier = Modifier) {
    val paths = remember { NotePaths.map { PathParser().parsePathString(it).toPath() } }
    val shape = RoundedCornerShape(size * 0.225f)
    Canvas(modifier.size(size).shadow(if (size > 60.dp) 14.dp else 0.dp, shape).clip(shape)) {
        val s = this.size.width / 108f
        scale(s, s, pivot = Offset.Zero) {
            drawRect(
                Brush.linearGradient(listOf(Color(0xFFE5173F), Color(0xFFFF7A9A)), Offset(0f, 0f), Offset(108f, 108f)),
                size = Size(108f, 108f)
            )
            drawCircle(Color.White.copy(alpha = 0.18f), radius = 32f, center = Offset(92f, 92f))
            // scaled up around the middle and nudged by (-2, 1) to centre the notes
            translate(-2f, 1f) {
                scale(1.12f, 1.12f, pivot = Offset(54f, 54f)) {
                    val fill = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFC9D5)), startY = 27f, endY = 79f)
                    paths.forEach { drawPath(it, fill) }
                }
            }
        }
    }
}
