package com.myshelf.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.*
import java.io.File

data class CoverResult(val itemId: String, val uri: String)

class CoverPickerViewModel(app: Application) : AndroidViewModel(app) {
    var busyFor by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf<CoverResult?>(null)
        private set
    var errorFor by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null
    private var generation = 0

    fun showError(id: String, text: String) { errorFor = id; error = text }
    fun clearError() { errorFor = null; error = null }
    fun consume() { result = null }
    fun cancel(id: String) {
        if (busyFor == id) { generation++; job?.cancel(); busyFor = null }
        if (result?.itemId == id) result = null
        if (errorFor == id) clearError()
    }

    fun choose(id: String, uri: Uri) = load(id) { checkCancelled ->
        require(uri.scheme == "content") { "Choose an image from your device." }
        val stream = requireNotNull(getApplication<Application>().contentResolver.openInputStream(uri)) { "Can't open this image." }
        stream.use { CoverInput.read(it, checkCancelled) }
    }
    fun fromLink(id: String, link: String) = load(id) { checkCancelled -> CoverInput.download(link, checkCancelled) }

    private fun load(id: String, read: (() -> Unit) -> ByteArray) {
        job?.cancel()
        val token = ++generation
        busyFor = id; clearError()
        job = viewModelScope.launch {
            var created: File? = null
            var delivered = false
            try {
                val uri = withContext(Dispatchers.IO) {
                    val context = currentCoroutineContext()
                    val bytes = read { context.ensureActive() }
                    context.ensureActive()
                    val file = storeImage(bytes)
                    created = file
                    context.ensureActive()
                    Uri.fromFile(file).toString()
                }
                if (token == generation) { result = CoverResult(id, uri); delivered = true }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (token == generation) showError(id, when (e) {
                    is IllegalArgumentException -> e.message ?: "Choose a valid image."
                    is java.net.SocketTimeoutException -> "Image download timed out. Try again."
                    is java.net.UnknownHostException -> "Can't connect. Check your internet connection."
                    else -> "Couldn't add this cover. Try another image or link."
                })
            } finally {
                if (!delivered) withContext(NonCancellable + Dispatchers.IO) { created?.delete() }
                if (token == generation) busyFor = null
            }
        }
    }

    private fun storeImage(bytes: ByteArray): File {
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
                val directory = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
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
