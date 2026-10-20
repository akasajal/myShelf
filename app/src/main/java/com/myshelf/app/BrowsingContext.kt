package com.myshelf.app

import androidx.compose.runtime.saveable.Saver
import org.json.JSONObject

data class BrowsingContext(val query: String = "", val media: String? = null)

internal val BrowsingContextsSaver = Saver<Map<String, BrowsingContext>, String>(
    save = { encodeBrowsingContexts(it) },
    restore = { decodeBrowsingContexts(it) }
)

internal fun encodeBrowsingContexts(contexts: Map<String, BrowsingContext>): String = JSONObject().apply {
    contexts.forEach { (route, context) -> put(route, JSONObject().put("query", context.query).put("media", context.media)) }
}.toString()

internal fun decodeBrowsingContexts(saved: String): Map<String, BrowsingContext> {
        val json = JSONObject(saved)
        return json.keys().asSequence().associateWith { route ->
            val context = json.getJSONObject(route)
            BrowsingContext(context.optString("query"), if (context.has("media")) context.getString("media") else null)
        }
}
