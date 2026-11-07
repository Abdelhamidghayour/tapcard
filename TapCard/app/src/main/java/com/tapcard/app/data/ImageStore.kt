package com.tapcard.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Persists locally-picked images (profile photos, logos) into app-internal
 * storage so cards keep showing them across restarts without any permission.
 *
 * Handles the two classic "my photo looks broken" problems:
 *  1. EXIF rotation — camera photos carry an orientation flag; raw decoding
 *     ignores it, so we rotate to upright before saving.
 *  2. Arbitrary size — downscale to [maxDim] on the longest edge and
 *     recompress, so display slots get a consistent, memory-safe image.
 */
object ImageStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "card_images").apply { mkdirs() }

    /** Copies, rotates upright, downscales and saves the picked image. */
    fun persist(context: Context, source: Uri, maxDim: Int = 1024): String? {
        return try {
            // 1) Bounds pass
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null

            // 2) Sampled decode
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // 3) Downscale to maxDim
            val scale = if (maxOf(src.width, src.height) > maxDim)
                maxDim.toFloat() / maxOf(src.width, src.height) else 1f
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(1),
                (src.height * scale).toInt().coerceAtLeast(1),
                true,
            ) else src

            // 4) EXIF rotation → upright
            val rotation = exifRotation(context, source)
            val upright = if (rotation != 0f) Bitmap.createBitmap(
                scaled, 0, 0, scaled.width, scaled.height,
                Matrix().apply { postRotate(rotation) }, true,
            ) else scaled

            // 5) Save
            val file = File(dir(context), "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            Uri.fromFile(file).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun exifRotation(context: Context, uri: Uri): Float {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> 360f // rare; skip flip, keep upright
            else -> 0f
        }
    }

    fun delete(context: Context, uriString: String?) {
        if (uriString == null) return
        runCatching {
            if (uriString.startsWith("file://")) {
                File(Uri.parse(uriString).path ?: "").delete()
            }
        }
    }
}
