package com.songbook.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * A list whose rows can be dragged into a new order. Put the drag [handle] modifier on the part of the row
 * that should start the drag (an icon on the right). Rows must all have the same height. [onMove] is called
 * every time the dragged row passes a neighbour, with the old and the new position.
 */
@Composable
fun <T> ReorderList(
    items: List<T>,
    key: (T) -> Long,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    rowContent: @Composable (item: T, dragging: Boolean, handle: Modifier) -> Unit
) {
    val state = rememberLazyListState()
    var dragKey by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val move by rememberUpdatedState(onMove)

    LazyColumn(state = state, modifier = modifier, contentPadding = contentPadding) {
        items(items, key = { key(it) }) { item ->
            val k = key(item)
            val dragging = dragKey == k
            Box(
                Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
            ) {
                val handle = Modifier.pointerInput(k) {
                    detectDragGestures(
                        onDragStart = { dragKey = k; dragOffset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val infos = state.layoutInfo.visibleItemsInfo
                            val me = infos.firstOrNull { it.key == k }
                            if (me != null) {
                                val center = me.offset + me.size / 2f + dragOffset
                                val target = infos.firstOrNull { it.key != k && center >= it.offset && center <= it.offset + it.size }
                                if (target != null) {
                                    move(me.index, target.index)
                                    // the row now sits where the neighbour was: keep it under the finger
                                    dragOffset += (me.offset - target.offset).toFloat()
                                }
                                // near the top or bottom edge: scroll along
                                val top = me.offset + dragOffset
                                val bottom = top + me.size
                                val dy = when {
                                    top < state.layoutInfo.viewportStartOffset + 80f -> -16f
                                    bottom > state.layoutInfo.viewportEndOffset - 80f -> 16f
                                    else -> 0f
                                }
                                if (dy != 0f) {
                                    scope.launch { state.scrollBy(dy) }
                                    dragOffset += dy
                                }
                            }
                        },
                        onDragEnd = { dragKey = null; dragOffset = 0f },
                        onDragCancel = { dragKey = null; dragOffset = 0f }
                    )
                }
                rowContent(item, dragging, handle)
            }
        }
    }
}
