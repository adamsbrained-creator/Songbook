package com.songbook.app.ui.components

import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.songbook.app.data.ImageStore
import com.songbook.app.ui.theme.Sb
import com.songbook.app.ui.theme.sbStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Small in-memory cache so scrolling lists does not decode the same cover again and again. */
private object CoverCache {
    private val cache = object : LruCache<String, ImageBitmap>(
        maxOf(4 * 1024, (Runtime.getRuntime().maxMemory() / 8 / 1024).toInt())
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4) / 1024
    }

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) { cache.put(key, value) }
}

/** Loads a stored cover (see [ImageStore]) off the main thread. Returns null while loading / when missing. */
@Composable
fun rememberCover(name: String?, targetPx: Int = 600): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val key = if (name == null) null else "$name@$targetPx"
    var bmp by remember(key) { mutableStateOf(if (key != null) CoverCache.get(key) else null) }
    LaunchedEffect(key) {
        if (key != null && name != null && bmp == null) {
            val loaded = withContext(Dispatchers.IO) {
                ImageStore.decodeScaled(context, name, targetPx)?.asImageBitmap()
            }
            if (loaded != null) {
                CoverCache.put(key, loaded)
                bmp = loaded
            }
        }
    }
    return bmp
}

/**
 * Returns a function that opens the system photo picker. The picked image is copied into app
 * storage and [onResult] receives the stored file name (or null if it could not be read).
 */
@Composable
fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { ImageStore.importFromUri(context, uri) }
                onResult(name)
            }
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

/** Preview + "Upload image" / "Remove image" + the gradient choices. Used by every artwork dialog. */
@Composable
fun ArtworkEditor(gradient: Int, image: String?, onChange: (gradient: Int, image: String?) -> Unit) {
    val c = Sb.colors
    val pick = rememberImagePicker { name -> if (name != null) onChange(gradient, name) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareArtwork(gradient, 76.dp, 22.dp, image = image)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipPill(if (image == null) "Upload image" else "Change image", active = true, onClick = pick)
                if (image != null) {
                    ChipPill("Remove image", active = false, onClick = { onChange(gradient, null) }, inactiveBg = c.track)
                }
            }
        }
        Text("Or pick a color", style = sbStyle(12, color = c.mu), modifier = Modifier.padding(top = 2.dp))
        GradientPicker(gradient) { onChange(it, null) }
    }
}
