package com.myshelf.app.domain.collections

import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.core.model.collectionKey
import com.myshelf.app.domain.backup.ImportMode
import com.myshelf.app.domain.backup.mergeShelf

fun validateCollectionProfiles(profiles: List<CollectionProfile>) {
    require(profiles.size <= 2000) { "Too many collections." }
    profiles.forEach {
        require(it.name.isNotBlank() && it.name == it.name.trim() && it.name.length <= 80) { "Invalid collection name." }
        require(it.description.length <= 1000 && it.id.isNotBlank() && it.id.length <= 200) { "Invalid collection details." }
    }
    require(profiles.map { collectionKey(it.name) }.distinct().size == profiles.size) { "Duplicate collection names." }
    require(profiles.map { it.id }.distinct().size == profiles.size) { "Duplicate collection IDs." }
}

data class CollectionChange(val items: List<ShelfItem>, val profiles: List<CollectionProfile>)

fun createCollection(items: List<ShelfItem>, profiles: List<CollectionProfile>, profile: CollectionProfile): List<CollectionProfile> {
    val clean = profile.copy(name = profile.name.trim(), description = profile.description.trim())
    val key = collectionKey(clean.name)
    require(items.none { collectionKey(it.collection) == key } && profiles.none { collectionKey(it.name) == key }) {
        "A collection with this name already exists."
    }
    val next = profiles + clean
    validateCollectionProfiles(next)
    return next
}

fun customizeCollection(items: List<ShelfItem>, profiles: List<CollectionProfile>, original: String,
    updated: CollectionProfile): CollectionChange {
    val oldKey = collectionKey(original)
    val clean = updated.copy(name = updated.name.trim(), description = updated.description.trim())
    validateCollectionProfiles(listOf(clean))
    val newKey = collectionKey(clean.name)
    require(newKey == oldKey || (items.none { collectionKey(it.collection) == newKey } &&
        profiles.none { collectionKey(it.name) == newKey })) { "A collection with this name already exists." }
    val previous = profiles.find { collectionKey(it.name) == oldKey } ?: CollectionProfile(original)
    val profile = clean.copy(id = previous.id)
    val next = profiles.filterNot { collectionKey(it.name) == oldKey } + profile
    validateCollectionProfiles(next)
    return CollectionChange(items.map { if (collectionKey(it.collection) == oldKey) it.copy(collection = profile.name) else it }, next)
}

fun mergeCollectionData(existingItems: List<ShelfItem>, existing: List<CollectionProfile>, incomingItems: List<ShelfItem>,
    incoming: List<CollectionProfile>, mode: ImportMode): CollectionChange {
    if (mode == ImportMode.REPLACE) return CollectionChange(incomingItems, incoming)
    val profiles = existing.toMutableList()
    val inferred = existingItems.map { it.collection.trim() }.filter { it.isNotBlank() }.distinctBy(::collectionKey).map { CollectionProfile(it) }
    incoming.forEach { profile ->
        val index = profiles.indexOfFirst { it.id == profile.id || collectionKey(it.name) == collectionKey(profile.name) }
        if (index < 0) {
            val previous = inferred.find { it.id == profile.id || collectionKey(it.name) == collectionKey(profile.name) }
            profiles += if (mode == ImportMode.SKIP_EXISTING && previous != null) previous else profile
        }
        else if (mode == ImportMode.UPDATE_MATCHING) profiles[index] = profile
    }
    validateCollectionProfiles(profiles)
    val renamed = if (mode == ImportMode.SKIP_EXISTING) existingItems else existingItems.map { item ->
        val old = existing.find { collectionKey(it.name) == collectionKey(item.collection) }
            ?: item.collection.takeIf { it.isNotBlank() }?.let { CollectionProfile(it.trim()) }
        val next = profiles.find { it.id == old?.id }
        if (old != null && next != null && old.name != next.name) {
            require(collectionKey(old.name) == collectionKey(next.name) || existingItems.none {
                collectionKey(it.collection) == collectionKey(next.name)
            }) { "A renamed collection conflicts with your shelf." }
            item.copy(collection = next.name)
        } else item
    }
    val incomingTitles = if (mode != ImportMode.SKIP_EXISTING) incomingItems else incomingItems.map { item ->
        val source = incoming.find { collectionKey(it.name) == collectionKey(item.collection) }
        val kept = profiles.find { it.id == source?.id || collectionKey(it.name) == collectionKey(source?.name.orEmpty()) }
        if (kept != null) item.copy(collection = kept.name) else item
    }
    return CollectionChange(mergeShelf(renamed, incomingTitles, mode), profiles)
}
