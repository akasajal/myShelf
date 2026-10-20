package com.myshelf.app

import java.util.Locale

enum class ItemSort(val label: String) {
    NEWEST("Recently added"), OLDEST("First added"),
    TITLE_ASC("Title: A–Z"), TITLE_DESC("Title: Z–A"),
    RATING_DESC("Highest rated"), RATING_ASC("Lowest rated"),
    YEAR_DESC("Year: newest"), YEAR_ASC("Year: oldest")
}

enum class CollectionSort(val label: String) {
    RECENT("Recent additions"), NAME_ASC("Name: A–Z"), NAME_DESC("Name: Z–A"),
    MOST_TITLES("Most titles"), FEWEST_TITLES("Fewest titles")
}

data class ShelfCollection(val key: String, val name: String, val items: List<ShelfItem>)

fun collectionKey(name: String): String = name.trim().lowercase(Locale.ROOT)

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

fun sortCollections(items: List<ShelfItem>, order: CollectionSort): List<ShelfCollection> {
    val groups = items.filter { it.collection.isNotBlank() }.groupBy { collectionKey(it.collection) }
        .map { (key, titles) -> ShelfCollection(key, titles.first().collection.trim(), titles) }
    val name = compareBy(String.CASE_INSENSITIVE_ORDER) { collection: ShelfCollection -> collection.name }
    return when (order) {
        CollectionSort.RECENT -> groups
        CollectionSort.NAME_ASC -> groups.sortedWith(name)
        CollectionSort.NAME_DESC -> groups.sortedWith(name.reversed())
        CollectionSort.MOST_TITLES -> groups.sortedWith(compareByDescending<ShelfCollection> { it.items.size }.then(name))
        CollectionSort.FEWEST_TITLES -> groups.sortedWith(compareBy<ShelfCollection> { it.items.size }.then(name))
    }
}
