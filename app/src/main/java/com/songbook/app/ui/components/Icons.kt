package com.songbook.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stroke icons drawn on a 24x24 grid, 1.8 stroke, round caps, like the mockup's SVGs. */
class IconSpec(
    val paths: List<String> = emptyList(),
    /** x, y, radius */
    val circles: List<Triple<Float, Float, Float>> = emptyList()
)

object SbIcons {
    val Search = IconSpec(listOf("M16 16l4 4"), listOf(Triple(11f, 11f, 6.5f)))
    val Heart = IconSpec(listOf("M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"))
    val More = IconSpec(circles = listOf(Triple(12f, 5f, 1f), Triple(12f, 12f, 1f), Triple(12f, 19f, 1f)))
    val ChevronDown = IconSpec(listOf("M6 9l6 6 6-6"))
    val ChevronRight = IconSpec(listOf("M9 6l6 6-6 6"))
    val Back = IconSpec(listOf("M15 6l-6 6 6 6"))
    val Close = IconSpec(listOf("M6 6l12 12M18 6L6 18"))
    val Plus = IconSpec(listOf("M12 5v14M5 12h14"))
    val Music = IconSpec(listOf("M9 18V5l11-2v13"), listOf(Triple(6f, 18f, 3f), Triple(17f, 16f, 3f)))
    val Albums = IconSpec(
        listOf("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"),
        listOf(Triple(12f, 12f, 4f), Triple(17.5f, 6.5f, 0.4f))
    )
    val Sliders = IconSpec(
        listOf("M4 8h8M16 8h4M4 16h2M10 16h10"),
        listOf(Triple(14f, 8f, 2f), Triple(8f, 16f, 2f))
    )
    val Check = IconSpec(listOf("M5 12.5l4.5 4.5L19 7.5"))
    val Grid = IconSpec(listOf("M4 4h6v6H4z", "M14 4h6v6h-6z", "M4 14h6v6H4z", "M14 14h6v6h-6z"))
    val Rows = IconSpec(
        listOf("M8 6h13M8 12h13M8 18h13"),
        listOf(Triple(3.5f, 6f, 1f), Triple(3.5f, 12f, 1f), Triple(3.5f, 18f, 1f))
    )
    val Image = IconSpec(
        listOf("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M21 16l-5-5-9 9"),
        listOf(Triple(8.5f, 8.5f, 1.5f))
    )
}

@Composable
fun StrokeIcon(
    spec: IconSpec,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    filled: Boolean = false
) {
    val parsed = remember(spec) { spec.paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            parsed.forEach { p ->
                if (filled) drawPath(p, tint)
                drawPath(p, tint, style = stroke)
            }
            spec.circles.forEach { (x, y, r) ->
                drawCircle(tint, radius = r, center = Offset(x, y), style = stroke)
            }
        }
    }
}
