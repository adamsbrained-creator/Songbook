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
    "M48.64,73.10C50.36,77.35 47.43,82.54 42.11,84.70C36.78,86.85 31.07,85.15 29.36,80.90C27.64,76.65 30.57,71.46 35.89,69.30C41.22,67.15 46.93,68.85 48.64,73.10z M77.64,66.60C79.36,70.85 76.43,76.04 71.11,78.20C65.78,80.35 60.07,78.65 58.36,74.40C56.64,70.15 59.57,64.96 64.89,62.80C70.22,60.65 75.93,62.35 77.64,66.60z M43.76,36.26L47.04,35.44Q48.40,35.10 48.40,36.50L48.40,76.60Q48.40,78.00 47.00,78.00L43.80,78.00Q42.40,78.00 42.40,76.60L42.40,38.00Q42.40,36.60 43.76,36.26z M72.76,28.26L76.04,27.44Q77.40,27.10 77.40,28.50L77.40,70.10Q77.40,71.50 76.00,71.50L72.80,71.50Q71.40,71.50 71.40,70.10L71.40,30.00Q71.40,28.60 72.76,28.26z M45.12,35.92L74.68,28.48Q77.40,27.80 77.40,30.60L77.40,37.60Q77.40,40.40 74.68,41.08L45.12,48.52Q42.40,49.20 42.40,46.40L42.40,39.40Q42.40,36.60 45.12,35.92z"

/**
 * In-app copy of the Songbook 2.5 icon: a clean red-pink tile, two beamed notes with round heads, a faint
 * shadow and a white-to-pink fade. The notes are drawn larger than in the launcher icon because the whole
 * tile is visible here.
 */
@Composable
fun AppIconArt(size: Dp, modifier: Modifier = Modifier) {
    val note = remember { PathParser().parsePathString(NotePath).toPath() }
    val shape = RoundedCornerShape(size * 0.225f)
    Canvas(modifier.size(size).shadow(if (size > 60.dp) 14.dp else 0.dp, shape).clip(shape)) {
        val s = this.size.width / 108f
        scale(s, s, pivot = Offset.Zero) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFFFF4E6C), Color(0xFFF8163E)), startY = 0f, endY = 108f),
                size = Size(108f, 108f)
            )
            translate(0f, -2.5f) {
                scale(1.3f, 1.3f, pivot = Offset(54f, 54f)) {
                    for ((dy, a) in listOf(3.4f to 0.08f, 1.8f to 0.12f)) {
                        translate(0f, dy) { drawPath(note, Color(0xFF78001E).copy(alpha = a)) }
                    }
                    drawPath(note, Brush.verticalGradient(listOf(Color.White, Color(0xFFFFBECE)), startY = 50f, endY = 82f))
                }
            }
        }
    }
}
