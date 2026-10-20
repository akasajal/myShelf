package com.myshelf.app.domain.library

import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.CollectionSort
import com.myshelf.app.core.model.ItemSort
import com.myshelf.app.core.model.ShelfCollection
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.core.model.collectionKey

fun sortShelf(items: List<ShelfItem>, order: ItemSort): List<ShelfItem> {
    val title = compareBy(String.CASE_INSENSITIVE_ORDER) { item: ShelfItem -> item.title }
    return when (order) {
        ItemSort.NEWEST -> items.toList()
        ItemSort.OLDEST -> items.asReversed().toList()
        ItemSort.TITLE_ASC -> items.sortedWith(title)
        ItemSort.TITLE_DESC -> items.sortedWith(title.reversed())
        ItemSort.RATING_DESC -> items.sortedWith(compareBy<ShelfItem> { it.rating == 0 }.thenByDescending { it.rating }.then(title))
        ItemSort.RATING_ASC -> items.sortedWith(compareBy<ShelfItem> { it.rating == 0 }.thenBy { it.rating }.then(title))
        ItemSort.YEAR_DESC -> items.sortedWith(compareBy<ShelfItem> { releaseYear(it) == null }.thenByDescending { releaseYear(it) }.then(title))
        ItemSort.YEAR_ASC -> items.sortedWith(compareBy<ShelfItem> { releaseYear(it) == null }.thenBy { releaseYear(it) }.then(title))
    }
}

private fun releaseYear(item: ShelfItem): Int? = item.year.trim().toIntOrNull()?.takeIf { it > 0 }

fun sortCollections(items: List<ShelfItem>, order: CollectionSort, profiles: List<CollectionProfile> = emptyList(),
    includeEmpty: Boolean = true): List<ShelfCollection> {
    val metadata = profiles.associateBy { collectionKey(it.name) }
    val groups = items.filter { it.collection.isNotBlank() }.groupBy { collectionKey(it.collection) }
        .map { (key, titles) ->
            val profile = metadata[key] ?: CollectionProfile(titles.first().collection.trim())
            ShelfCollection(key, profile.name, titles, profile)
        }.toMutableList()
    if (includeEmpty) profiles.filter { profile -> groups.none { it.key == collectionKey(profile.name) } }.forEach {
        groups += ShelfCollection(collectionKey(it.name), it.name, emptyList(), it)
    }
    val name = compareBy(String.CASE_INSENSITIVE_ORDER) { collection: ShelfCollection -> collection.name }
    return when (order) {
        CollectionSort.RECENT -> groups
        CollectionSort.NAME_ASC -> groups.sortedWith(name)
        CollectionSort.NAME_DESC -> groups.sortedWith(name.reversed())
        CollectionSort.MOST_TITLES -> groups.sortedWith(compareByDescending<ShelfCollection> { it.items.size }.then(name))
        CollectionSort.FEWEST_TITLES -> groups.sortedWith(compareBy<ShelfCollection> { it.items.size }.then(name))
    }
}
