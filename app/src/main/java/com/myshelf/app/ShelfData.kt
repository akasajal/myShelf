package com.myshelf.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONArray
import java.util.UUID

enum class MediaType(val label: String, val singular: String) {
    MOVIE("Movies", "Movie"), SERIES("Series", "Series"), TV("TV shows", "TV show"),
    COMIC("Comics", "Comic"), BOOK("Books", "Book"), OTHER("Other", "Other")
}
enum class Progress(val label: String) {
    PLANNED("On my list"), ACTIVE("In progress"), FINISHED("Finished"), PAUSED("On hold")
}
data class ShelfItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "", val type: MediaType = MediaType.MOVIE,
    val genre: String = "", val collection: String = "", val cover: String = "",
    val year: String = "", val creator: String = "", val rating: Int = 0,
    val progress: Progress = Progress.PLANNED, val notes: String = ""
)

fun filterShelf(items: List<ShelfItem>, type: MediaType?, genre: String?, collection: String?, query: String): List<ShelfItem> =
    items.filter { item ->
        (type == null || item.type == type) &&
            (genre == null || (type != null && item.type == type && item.genre.equals(genre, true))) &&
            (collection == null || item.collection.equals(collection, true)) &&
            (query.isBlank() || listOf(item.title, item.creator, item.collection).any { it.contains(query.trim(), true) })
    }

class ShelfViewModel(app: Application) : AndroidViewModel(app) {
    var editing by mutableStateOf<ShelfItem?>(null)
    private val prefs = app.getSharedPreferences("shelf", 0)
    var themeMode by mutableStateOf(runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM))
        private set
    var accent by mutableStateOf(runCatching { Accent.valueOf(prefs.getString("accent", "ROSE")!!) }.getOrDefault(Accent.ROSE))
        private set
    var cardsPerRow by mutableIntStateOf(prefs.getInt("cards_per_row", 2).coerceIn(2, 4))
        private set
    var transferBusy by mutableStateOf(false)
        private set
    var transferMessage by mutableStateOf<String?>(null)
        private set
    var pendingImport by mutableStateOf<ImportedShelf?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var items by mutableStateOf(readItems())
        private set

    private fun readItems(): List<ShelfItem> = try {
        ShelfJson.decode(JSONArray(prefs.getString("items", "[]")))
    } catch (_: Exception) {
        loadError = "Couldn't load your shelf. Restart the app to try again."
        emptyList()
    }

    fun save(item: ShelfItem) {
        if (loadError != null || transferBusy) return
        items = if (items.any { it.id == item.id }) items.map { if (it.id == item.id) item else it }
        else listOf(item) + items
        persist()
    }
    fun delete(id: String) { if (loadError == null && !transferBusy) { items = items.filterNot { it.id == id }; persist() } }
    private fun persist() {
        prefs.edit().putString("items", ShelfJson.encode(items).toString()).apply()
    }

    fun setTheme(mode: ThemeMode) { themeMode = mode; prefs.edit().putString("theme", mode.name).apply() }
    fun updateAccent(value: Accent) { accent = value; prefs.edit().putString("accent", value.name).apply() }
    fun updateCardsPerRow(count: Int) {
        require(count in 2..4)
        cardsPerRow = count
        prefs.edit().putInt("cards_per_row", count).apply()
    }
    fun dismissImport() { if (!transferBusy) pendingImport = null }

    fun exportShelf(uri: Uri) {
        if (transferBusy || loadError != null) return
        val snapshot = items.toList()
        transferBusy = true; transferMessage = null
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val output = requireNotNull(resolver.openOutputStream(uri, "wt"))
                    output.use { ShelfBackup.write(it, snapshot) { cover -> resolver.openInputStream(Uri.parse(cover)) } }
                }
                transferMessage = "Exported ${result.titles} titles." + if (result.missingCovers > 0)
                    " ${result.missingCovers} unavailable covers skipped." else ""
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { transferMessage = "Export failed. ${e.message ?: "Couldn't write the backup."} The selected file may be incomplete." }
            finally { transferBusy = false }
        }
    }

    fun previewImport(uri: Uri) {
        if (transferBusy || loadError != null) return
        transferBusy = true; transferMessage = null; pendingImport = null
        viewModelScope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val backup = requireNotNull(resolver.openInputStream(uri)).use { ShelfBackup.read(it) }
                    backup.covers.values.forEach { bytes ->
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        require(options.outWidth > 0 && options.outHeight > 0) { "Invalid cover image." }
                    }
                    backup
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { transferMessage = "Can't read this backup. Use a My Shelf ZIP under 64 MB unpacked." }
            finally { transferBusy = false }
        }
    }

    fun importShelf(replace: Boolean) {
        val backup = pendingImport ?: return
        if (transferBusy || loadError != null) return
        val previous = items
        val incoming = if (replace) backup.items else backup.items.filter { item -> previous.none { it.id == item.id } }
        transferBusy = true; transferMessage = null
        viewModelScope.launch {
            val created = mutableListOf<File>()
            try {
                val next = withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
                    val imported = incoming.map { item ->
                        if (item.cover.isBlank()) item else {
                            val file = File(dir, "${UUID.randomUUID()}.img")
                            created += file
                            file.writeBytes(backup.covers.getValue(item.cover))
                            item.copy(cover = Uri.fromFile(file).toString())
                        }
                    }
                    val combined = if (replace) imported else previous + imported
                    check(prefs.edit().putString("items", ShelfJson.encode(combined).toString()).commit()) { "Couldn't save imported titles." }
                    combined
                }
                items = next; pendingImport = null
                transferMessage = if (replace) "Restored ${incoming.size} titles." else
                    "Imported ${incoming.size} titles; skipped ${backup.items.size - incoming.size} existing titles."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                withContext(Dispatchers.IO) { created.forEach { it.delete() } }
                transferMessage = "Couldn't save the import. Your shelf is unchanged."
            } finally { transferBusy = false }
        }
    }
}
