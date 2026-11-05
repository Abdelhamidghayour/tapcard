package com.tapcard.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Persists locally-picked images (profile photos, logos) into app-internal
 * storage so cards keep showing them across restarts without any permission.
 * Cloud mode additionally mirrors to Firebase Storage via CardRepository.
 */
object ImageStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "card_images").apply { mkdirs() }

    /** Copies+downscales an picked image into internal storage; returns a file:// Uri string. */
    fun persist(context: Context, source: Uri, maxDim: Int = 800): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val scale = if (maxOf(src.width, src.height) > maxDim)
                maxDim.toFloat() / maxOf(src.width, src.height) else 1f
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(1),
                (src.height * scale).toInt().coerceAtLeast(1),
                true,
            ) else src

            val file = File(dir(context), "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            Uri.fromFile(file).toString()
        } catch (_: Exception) {
            null
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
