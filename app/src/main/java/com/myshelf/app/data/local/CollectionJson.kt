package com.myshelf.app.data.local

import com.myshelf.app.core.model.CollectionIcon
import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.domain.collections.validateCollectionProfiles
import org.json.JSONArray
import org.json.JSONObject

object CollectionJson {
    fun encode(profiles: List<CollectionProfile>): JSONArray = JSONArray().apply {
        profiles.forEach { put(JSONObject().put("id", it.id).put("name", it.name)
            .put("description", it.description).put("icon", it.icon.name)) }
    }

    fun decode(data: JSONArray): List<CollectionProfile> {
        require(data.length() <= 2000) { "Too many collections." }
        val profiles = List(data.length()) { index ->
            val o = data.getJSONObject(index)
            fun text(key: String): String {
                val value = o.get(key)
                require(value is String) { "Invalid collection $key." }
                return value
            }
            CollectionProfile(text("name"), text("description"), CollectionIcon.valueOf(text("icon")), text("id"))
        }
        validateCollectionProfiles(profiles)
        return profiles
    }
}
