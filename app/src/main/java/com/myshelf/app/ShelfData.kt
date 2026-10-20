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
    PLANNED("On my list"), ACTIVE("In progress"), FINISHED("Finished"), PAUSED("On hold"), DROPPED("Dropped")
}
data class ShelfItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "", val type: MediaType = MediaType.MOVIE,
    val genre: String = "", val collection: String = "", val cover: String = "",
    val year: String = "", val creator: String = "", val rating: Int = 0,
    val progress: Progress = Progress.PLANNED, val notes: String = "",
    val seasons: List<Season> = emptyList(), val seriesEnded: Boolean = false, val seriesDropped: Boolean = false
)

fun filterShelf(items: List<ShelfItem>, type: MediaType?, genre: String?, collection: String?, query: String,
    status: StatusFilter? = null): List<ShelfItem> =
    items.filter { item ->
        (type == null || item.type == type) &&
            (genre == null || (type != null && item.type == type && item.genre.equals(genre, true))) &&
            (collection == null || collectionKey(item.collection) == collectionKey(collection)) &&
            (status == null || status.matches(item)) &&
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
    var cardDetails by mutableStateOf(runCatching { CardDetails.valueOf(prefs.getString("card_details", "COMFORTABLE")!!) }.getOrDefault(CardDetails.COMFORTABLE))
        private set
    var shelfSort by mutableStateOf(runCatching { ItemSort.valueOf(prefs.getString("sort_shelf", "NEWEST")!!) }.getOrDefault(ItemSort.NEWEST))
        private set
    var collectionsSort by mutableStateOf(runCatching { CollectionSort.valueOf(prefs.getString("sort_collections", "RECENT")!!) }.getOrDefault(CollectionSort.RECENT))
        private set
    private var collectionSorts by mutableStateOf(prefs.all.mapNotNull { (key, value) ->
        if (key.startsWith("sort_inside_collection:")) runCatching {
            key.removePrefix("sort_inside_collection:") to ItemSort.valueOf(value as String)
        }.getOrNull() else null
    }.toMap())
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
    var collectionProfiles by mutableStateOf(readCollectionProfiles())
        private set

    fun collectionProfile(name: String): CollectionProfile = collectionProfiles.find { collectionKey(it.name) == collectionKey(name) }
        ?: CollectionProfile(name.trim())

    private fun readCollectionProfiles(): List<CollectionProfile> = try {
        CollectionJson.decode(JSONArray(prefs.getString("collections", "[]")))
    } catch (_: Exception) {
        loadError = "Couldn't load your collections. Restart the app to try again."
        emptyList()
    }

    fun addCollection(profile: CollectionProfile): String? {
        if (loadError != null || transferBusy) return "Your shelf is busy. Try again."
        return try {
            val next = createCollection(items, collectionProfiles, profile)
            prefs.edit().putString("collections", CollectionJson.encode(next).toString()).apply()
            collectionProfiles = next
            null
        } catch (e: IllegalArgumentException) { e.message ?: "Couldn't create this collection." }
    }

    fun updateCollection(original: String, updated: CollectionProfile): String? {
        if (loadError != null || transferBusy) return "Your shelf is busy. Try again."
        return try {
            val next = customizeCollection(items, collectionProfiles, original, updated)
            val oldKey = collectionKey(original)
            val newKey = collectionKey(updated.name)
            val sort = collectionSorts[oldKey]
            val nextSorts = (collectionSorts - oldKey).let { if (sort == null) it else it + (newKey to sort) }
            val editor = prefs.edit().putString("items", ShelfJson.encode(next.items).toString())
                .putString("collections", CollectionJson.encode(next.profiles).toString())
                .remove("sort_inside_collection:$oldKey")
            if (sort != null) editor.putString("sort_inside_collection:$newKey", sort.name)
            editor.apply()
            items = next.items; collectionProfiles = next.profiles; collectionSorts = nextSorts
            null
        } catch (e: IllegalArgumentException) { e.message ?: "Couldn't update this collection." }
    }

    private fun readItems(): List<ShelfItem> = try {
        ShelfJson.decode(JSONArray(prefs.getString("items", "[]")))
    } catch (_: Exception) {
        loadError = "Couldn't load your shelf. Restart the app to try again."
        emptyList()
    }

    fun save(item: ShelfItem) {
        if (loadError != null || transferBusy) return
        val normalized = item.copy(progress = item.seasonProgress())
        items = if (items.any { it.id == item.id }) items.map { if (it.id == item.id) normalized else it }
        else listOf(normalized) + items
        persist()
    }
    fun delete(id: String): RemovedTitle? {
        if (loadError != null || transferBusy) return null
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = RemovedTitle(items[index], index)
        items = items.filterNot { it.id == id }; persist()
        return removed
    }
    fun undoDelete(removed: RemovedTitle) {
        if (loadError != null || transferBusy) return
        items = restoreTitle(items, removed); persist()
    }
    private fun persist() {
        prefs.edit().putString("items", ShelfJson.encode(items).toString())
            .putString("collections", CollectionJson.encode(collectionProfiles).toString()).apply()
    }

    fun setTheme(mode: ThemeMode) { themeMode = mode; prefs.edit().putString("theme", mode.name).apply() }
    fun updateAccent(value: Accent) { accent = value; prefs.edit().putString("accent", value.name).apply() }
    fun updateCardsPerRow(count: Int) {
        require(count in 2..4)
        cardsPerRow = count
        prefs.edit().putInt("cards_per_row", count).apply()
    }
    fun updateCardDetails(value: CardDetails) {
        cardDetails = value; prefs.edit().putString("card_details", value.name).apply()
    }
    fun updateShelfSort(order: ItemSort) { shelfSort = order; prefs.edit().putString("sort_shelf", order.name).apply() }
    fun updateCollectionsSort(order: CollectionSort) { collectionsSort = order; prefs.edit().putString("sort_collections", order.name).apply() }
    fun sortInsideCollection(name: String): ItemSort = collectionSorts[collectionKey(name)] ?: ItemSort.NEWEST
    fun updateCollectionSort(name: String, order: ItemSort) {
        val key = collectionKey(name)
        collectionSorts = collectionSorts + (key to order)
        prefs.edit().putString("sort_inside_collection:$key", order.name).apply()
    }
    fun dismissImport() { if (!transferBusy) pendingImport = null }

    fun exportShelf(uri: Uri) {
        if (transferBusy || loadError != null) return
        val snapshot = items.toList()
        val collectionSnapshot = collectionProfiles.toList()
        transferBusy = true; transferMessage = null
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val output = requireNotNull(resolver.openOutputStream(uri, "wt"))
                    output.use { ShelfBackup.write(it, snapshot, collectionSnapshot) { cover -> resolver.openInputStream(Uri.parse(cover)) } }
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

    fun importShelf(mode: ImportMode) {
        val backup = pendingImport ?: return
        if (transferBusy || loadError != null) return
        val previous = items
        val previousCollections = collectionProfiles
        val changes = previewChanges(previous, backup.items)
        val incoming = if (mode == ImportMode.SKIP_EXISTING) changes.added else backup.items
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
                    val combined = mergeCollectionData(previous, previousCollections, imported, backup.collections, mode)
                    check(prefs.edit().putString("items", ShelfJson.encode(combined.items).toString())
                        .putString("collections", CollectionJson.encode(combined.profiles).toString()).commit()) { "Couldn't save imported titles." }
                    combined
                }
                items = next.items; collectionProfiles = next.profiles; pendingImport = null
                transferMessage = when (mode) {
                    ImportMode.REPLACE -> "Restored ${incoming.size} titles."
                    ImportMode.UPDATE_MATCHING -> "Added ${changes.added.size} titles; updated ${changes.matching.size}."
                    ImportMode.SKIP_EXISTING -> "Added ${changes.added.size} titles; skipped ${changes.matching.size} existing titles."
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                withContext(Dispatchers.IO) { created.forEach { it.delete() } }
                transferMessage = "Couldn't save the import. Your shelf is unchanged."
            } finally { transferBusy = false }
        }
    }
}
