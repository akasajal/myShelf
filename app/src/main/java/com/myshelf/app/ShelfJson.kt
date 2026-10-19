package com.myshelf.app

import org.json.JSONArray
import org.json.JSONObject

object ShelfJson {
    fun encode(items: List<ShelfItem>): JSONArray = JSONArray().apply {
        items.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("title", item.title); put("type", item.type.name)
            put("genre", item.genre); put("collection", item.collection); put("cover", item.cover)
            put("year", item.year); put("creator", item.creator); put("rating", item.rating)
            put("progress", item.progress.name); put("notes", item.notes)
        }) }
    }

    fun decode(data: JSONArray): List<ShelfItem> {
        val items = List(data.length()) { index ->
            val o = data.getJSONObject(index)
            fun text(key: String): String {
                if (!o.has(key)) return ""
                val value = o.get(key)
                require(value is String) { "Invalid $key in backup." }
                return value
            }
            val rating = o.get("rating")
            require(rating is Number && rating.toDouble() in 0.0..5.0 && rating.toDouble() == rating.toInt().toDouble()) { "Invalid rating in backup." }
            ShelfItem(
                id = text("id").also { require(it.isNotBlank()) { "Missing title ID." } },
                title = text("title").also { require(it.isNotBlank()) { "Missing title." } },
                type = MediaType.valueOf(text("type")), genre = text("genre"),
                collection = text("collection"), cover = text("cover"),
                year = text("year"), creator = text("creator"), rating = rating.toInt(),
                progress = Progress.valueOf(text("progress")), notes = text("notes")
            )
        }
        require(items.map { it.id }.distinct().size == items.size) { "Duplicate title IDs in backup." }
        return items
    }
}
