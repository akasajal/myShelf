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
            put("seasons", encodeSeasons(item.seasons)); put("seriesEnded", item.seriesEnded); put("seriesDropped", item.seriesDropped)
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
                progress = Progress.valueOf(text("progress")), notes = text("notes"),
                seasons = if (o.has("seasons")) decodeSeasons(o.getJSONArray("seasons")) else emptyList(),
                seriesEnded = if (o.has("seriesEnded")) {
                    val ended = o.get("seriesEnded")
                    require(ended is Boolean) { "Invalid series ending in backup." }
                    ended
                } else false,
                seriesDropped = if (o.has("seriesDropped")) {
                    val dropped = o.get("seriesDropped")
                    require(dropped is Boolean) { "Invalid dropped series state in backup." }
                    dropped
                } else false
            ).let { it.copy(progress = it.seasonProgress()) }
        }
        require(items.map { it.id }.distinct().size == items.size) { "Duplicate title IDs in backup." }
        return items
    }

    fun encodeSeasons(seasons: List<Season>): JSONArray = JSONArray().apply {
        seasons.forEach { season -> put(JSONObject().apply {
            put("number", season.number); put("progress", season.progress.name)
            put("rating", season.rating); put("watched", season.watched); put("episodes", season.episodes)
            put("name", season.name)
        }) }
    }

    fun decodeSeasons(data: JSONArray): List<Season> {
        require(data.length() <= 500) { "Too many seasons." }
        val seasons = List(data.length()) { index ->
            val o = data.getJSONObject(index)
            fun integer(key: String, range: IntRange): Int {
                val value = o.get(key)
                require(value is Number && value.toDouble() == value.toInt().toDouble() && value.toInt() in range) { "Invalid season $key." }
                return value.toInt()
            }
            Season(integer("number", 1..999), Progress.valueOf(o.getString("progress")),
                integer("rating", 0..5), integer("watched", 0..9999), integer("episodes", 0..9999),
                if (o.has("name")) {
                    val name = o.get("name")
                    require(name is String && name.length <= 200) { "Invalid season name." }
                    name
                } else "").also {
                require(it.episodes == 0 || it.watched <= it.episodes) { "Watched episodes exceed the season total." }
                require(it.progress != Progress.FINISHED || it.episodes == 0 || it.watched == it.episodes) { "Finished season has unwatched episodes." }
            }
        }
        require(seasons.map { it.number }.distinct().size == seasons.size) { "Duplicate season numbers." }
        return seasons.sortedBy { it.number }
    }
}
