package com.songbook.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Cover images live in the app's private storage (filesDir/covers). Entities only store the file
 * name. Picked images are copied, rotated by their EXIF flag and scaled down to [MAX_SIDE] px.
 */
object ImageStore {
    private const val MAX_SIDE = 1024

    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "covers").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** Copies a picked image into app storage. Returns the new file name or null on failure. */
    fun importFromUri(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.applicationContext.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val rotation = resolver.openInputStream(uri)?.use { readRotation(it) } ?: 0
            val matrix = Matrix()
            val longest = maxOf(bmp.width, bmp.height)
            if (longest > MAX_SIDE) {
                val s = MAX_SIDE.toFloat() / longest
                matrix.postScale(s, s)
            }
            if (rotation != 0) matrix.postRotate(rotation.toFloat())
            if (!matrix.isIdentity) {
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            }
            val png = bmp.hasAlpha()
            val name = "img_${UUID.randomUUID()}." + if (png) "png" else "jpg"
            FileOutputStream(file(context, name)).use {
                if (png) bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                else bmp.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
            name
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun readRotation(input: InputStream): Int = try {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    /** Decodes a stored image so its short side is at least [targetPx] (keeps memory low for thumbnails). */
    fun decodeScaled(context: Context, name: String, targetPx: Int): Bitmap? {
        val f = file(context, name)
        if (!f.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun readBytes(context: Context, name: String): ByteArray? =
        runCatching { file(context, name).takeIf { it.exists() }?.readBytes() }.getOrNull()

    fun saveBytes(context: Context, bytes: ByteArray): String? = runCatching {
        val name = "img_${UUID.randomUUID()}.jpg"
        file(context, name).writeBytes(bytes)
        name
    }.getOrNull()

    /** Deletes cover files that no song, album or group uses any more. */
    fun cleanup(context: Context, used: Set<String>) {
        dir(context).listFiles()?.forEach { f ->
            if (f.name.startsWith("img_") && f.name !in used) f.delete()
        }
    }
}
