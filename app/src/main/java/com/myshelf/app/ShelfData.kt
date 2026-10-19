package com.myshelf.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
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
    var loadError by mutableStateOf<String?>(null)
        private set
    var items by mutableStateOf(readItems())
        private set

    private fun readItems(): List<ShelfItem> = try {
        val data = JSONArray(prefs.getString("items", "[]"))
        List(data.length()) { index ->
            val o = data.getJSONObject(index)
            ShelfItem(
                id = o.getString("id"), title = o.getString("title"),
                type = MediaType.valueOf(o.getString("type")), genre = o.optString("genre"),
                collection = o.optString("collection"), cover = o.optString("cover"), year = o.optString("year"),
                creator = o.optString("creator"), rating = o.optInt("rating").coerceIn(0, 5),
                progress = Progress.valueOf(o.getString("progress")), notes = o.optString("notes")
            )
        }
    } catch (_: Exception) {
        loadError = "Your saved shelf could not be loaded. Restart before making changes to avoid replacing it."
        emptyList()
    }

    fun save(item: ShelfItem) {
        if (loadError != null) return
        items = if (items.any { it.id == item.id }) items.map { if (it.id == item.id) item else it }
        else listOf(item) + items
        persist()
    }
    fun delete(id: String) { if (loadError == null) { items = items.filterNot { it.id == id }; persist() } }
    private fun persist() {
        val data = JSONArray()
        items.forEach { item ->
            data.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("type", item.type.name)
                put("genre", item.genre); put("collection", item.collection); put("cover", item.cover)
                put("year", item.year); put("creator", item.creator); put("rating", item.rating)
                put("progress", item.progress.name); put("notes", item.notes)
            })
        }
        prefs.edit().putString("items", data.toString()).apply()
    }
}
