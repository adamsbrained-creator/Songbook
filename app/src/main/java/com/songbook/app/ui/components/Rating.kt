package com.songbook.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ratings go from 0 to 5 in steps of 0.25. */
fun quantizeRating(v: Float): Float = ((v.coerceIn(0f, 5f) * 4f).roundToInt() / 4f)

/** 4.75 -> "4.75", 4.5 -> "4.5", 4.0 -> "4" */
fun ratingText(v: Float): String {
    val q = quantizeRating(v)
    return if (q % 1f == 0f) q.toInt().toString() else if ((q * 4) % 2f == 0f) "%.1f".format(java.util.Locale.US, q) else "%.2f".format(java.util.Locale.US, q)
}

private fun starPath(cx: Float, cy: Float, outer: Float): Path {
    val inner = outer * 0.52f
    val p = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

/**
 * Five literal stars. Whole stars are filled, empty ones are outlines and the star in between is
 * filled from the left by 25, 50 or 75 percent, so 4.75 shows four stars and three quarters of the fifth.
 */
@Composable
fun StarRow(
    value: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp,
    gap: Dp = 3.dp,
    color: Color = Sb.colors.tx
) {
    val v = quantizeRating(value)
    Canvas(modifier.size(starSize * 5 + gap * 4, starSize)) {
        val s = starSize.toPx()
        val g = gap.toPx()
        val outer = s * 0.5f
        val cy = s * 0.5f + outer * 0.095f
        val stroke = Stroke(width = s * 0.08f, join = StrokeJoin.Round)
        for (i in 0 until 5) {
            val left = i * (s + g)
            val path = starPath(left + s / 2f, cy, outer * 0.94f)
            val fill = (v - i).coerceIn(0f, 1f)
            if (fill < 1f) drawPath(path, color.copy(alpha = 0.55f), style = stroke)
            if (fill > 0f) {
                clipRect(left = left - 1f, top = 0f, right = left + s * fill, bottom = s) {
                    drawPath(path, color)
                    drawPath(path, color, style = stroke)
                }
            }
        }
    }
}

/** One filled star with the number next to it, for list rows and tiles: "★ 4.75". */
@Composable
fun RatingTag(value: Float, modifier: Modifier = Modifier, color: Color = Sb.colors.mu) {
    if (value <= 0f) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(Modifier.size(11.dp)) {
            val s = size.width
            drawPath(starPath(s / 2f, s * 0.54f, s * 0.5f), color)
        }
        Text(ratingText(value), style = sbStyle(11, FontWeight.Medium, color))
    }
}

/** Stars you can tap or drag over to set the rating in quarter steps. */
@Composable
fun RatingEditor(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier, starSize: Dp = 44.dp) {
    val gap = 6.dp
    val total = starSize * 5 + gap * 4
    val widthPx = with(LocalDensity.current) { total.toPx() }
    val current by rememberUpdatedState(onChange)
    fun at(x: Float) = quantizeRating(x / widthPx * 5f)
    Box(
        modifier
            .pointerInput(widthPx) { detectTapGestures { current(at(it.x)) } }
            .pointerInput(widthPx) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    current(at(change.position.x))
                }
            }
    ) { StarRow(value, starSize = starSize, gap = gap) }
}

@Composable
fun RatingDialog(title: String, value: Float, onSave: (Float) -> Unit, onDismiss: () -> Unit) {
    val c = Sb.colors
    var v by remember { mutableFloatStateOf(quantizeRating(value)) }
    SbDialog(title, onDismiss, "Save", { onSave(v) }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (v <= 0f) "Not rated" else ratingText(v) + " / 5",
                style = sbStyle(28, FontWeight.Bold, if (v <= 0f) c.mu else c.ac),
                textAlign = TextAlign.Center
            )
            RatingEditor(v, { v = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipPill("− 0.25", active = false, onClick = { v = quantizeRating(v - 0.25f) }, inactiveBg = c.track)
                ChipPill("+ 0.25", active = false, onClick = { v = quantizeRating(v + 0.25f) }, inactiveBg = c.track)
                ChipPill("Clear", active = false, onClick = { v = 0f }, inactiveBg = c.track)
            }
        }
    }
}

/** "Rating  ★★★★☆ 4.25 >" row for edit dialogs. */
@Composable
fun RatingRow(value: Float, onClick: () -> Unit) {
    PickRow("Rating", onClick) {
        if (value <= 0f) Text("Not rated", style = sbStyle(14, color = Sb.colors.mu))
        else StarRow(value, starSize = 14.dp, gap = 2.dp)
    }
}
