package com.tapcard.app.data.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Real QR generation via ZXing (no placeholders). The QR encodes the card's
 * public URL, so any phone camera opens the digital business card.
 */
object QrCodeGenerator {

    fun generate(content: String, sizePx: Int = 720): Bitmap {
        require(content.isNotBlank()) { "QR content must not be blank" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) {
                pixels[row + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    /** Writes the PNG used for sharing; returns the cached file. */
    fun writePng(bitmap: Bitmap, context: Context, slug: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "tapcard-qr-$slug.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /** Fires the Android share sheet with the QR image + URL text. */
    fun share(context: Context, bitmap: Bitmap, slug: String, url: String) {
        val file = writePng(bitmap, context, slug)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Here's my digital business card: $url")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share business card"))
    }

    /**
     * Saves the QR into MediaStore (Pictures/TapCard). Scoped storage works
     * without permissions on API 29+; on API 26–28 this is intentionally a
     * no-op returning false rather than requesting a legacy permission.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, slug: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 29) return false
        val values = ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "tapcard-qr-$slug.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/TapCard",
            )
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection =
            android.provider.MediaStore.Images.Media.getContentUri(
                android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
            )
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
