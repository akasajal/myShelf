package com.myshelf.app.features.cover

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myshelf.app.data.cover.CoverInput
import com.myshelf.app.data.cover.CoverStorage
import java.io.File
import kotlinx.coroutines.*

data class CoverResult(val itemId: String, val uri: String)

class CoverPickerViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = CoverStorage(app.filesDir)
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
                    val file = storage.store(bytes)
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

}
