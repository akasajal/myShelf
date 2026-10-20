package com.myshelf.app.data.cover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Decodes, orients, resizes, and stores device-private cover images. */
class CoverStorage(private val filesDir: File) {
    fun store(bytes: ByteArray): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This link or clipboard item isn't a supported image." }
        require(bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000) { "Choose an image with smaller dimensions." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })) { "Can't read this image." }
        var oriented = decoded
        try {
            val exif = runCatching { ExifInterface(bytes.inputStream()) }.getOrNull()
            if (exif != null && (exif.isFlipped || exif.rotationDegrees != 0)) {
                val matrix = Matrix().apply {
                    if (exif.isFlipped) postScale(-1f, 1f)
                    postRotate(exif.rotationDegrees.toFloat())
                }
                oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            val ratio = minOf(1f, 1600f / maxOf(oriented.width, oriented.height))
            val width = (oriented.width * ratio).toInt().coerceAtLeast(1)
            val height = (oriented.height * ratio).toInt().coerceAtLeast(1)
            val cover = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                Canvas(cover).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(oriented, null, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
                }
                val directory = File(filesDir, "covers").apply { mkdirs() }
                val file = File.createTempFile("cover-", ".jpg", directory)
                try {
                    file.outputStream().use { check(cover.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                    return file
                } catch (e: Exception) { file.delete(); throw e }
            } finally { cover.recycle() }
        } finally {
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }
}
