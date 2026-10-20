package com.myshelf.app

import org.junit.Assert.*
import org.junit.Test

class ShelfSortingTest {
    private val newest = ShelfItem(id = "new", title = "zebra", rating = 0, year = "unknown", collection = "Marvel")
    private val middle = ShelfItem(id = "mid", title = "Alpha", rating = 5, year = "2024", collection = "DC")
    private val oldest = ShelfItem(id = "old", title = "beta", rating = 2, year = "1998", collection = "marvel")
    private val items = listOf(newest, middle, oldest)

    @Test fun addedOrderIsPreservedAndReversibleWithoutMutatingData() {
        assertEquals(items, sortShelf(items, ItemSort.NEWEST))
        assertEquals(listOf(oldest, middle, newest), sortShelf(items, ItemSort.OLDEST))
        assertEquals(listOf("new", "mid", "old"), items.map { it.id })
    }
    @Test fun alphabeticSortingIgnoresCaseInBothDirections() {
        assertEquals(listOf(middle, oldest, newest), sortShelf(items, ItemSort.TITLE_ASC))
        assertEquals(listOf(newest, oldest, middle), sortShelf(items, ItemSort.TITLE_DESC))
    }
    @Test fun unratedTitlesAlwaysFollowRatedTitles() {
        assertEquals(listOf(middle, oldest, newest), sortShelf(items, ItemSort.RATING_DESC))
        assertEquals(listOf(oldest, middle, newest), sortShelf(items, ItemSort.RATING_ASC))
    }
    @Test fun unknownYearsAlwaysFollowKnownYears() {
        val blank = newest.copy(id = "blank", title = "A blank", year = "")
        val values = listOf(newest, blank, middle, oldest)
        assertEquals(listOf(middle, oldest, blank, newest), sortShelf(values, ItemSort.YEAR_DESC))
        assertEquals(listOf(oldest, middle, blank, newest), sortShelf(values, ItemSort.YEAR_ASC))
    }
    @Test fun sortingDoesNotUndoMediaAndGenreFilters() {
        val book = middle.copy(id = "book", type = MediaType.BOOK, genre = "Romance")
        val movie = oldest.copy(id = "movie", type = MediaType.MOVIE, genre = "Romance")
        val filtered = filterShelf(listOf(book, movie), MediaType.BOOK, "Romance", null, "")
        assertEquals(listOf(book), sortShelf(filtered, ItemSort.TITLE_ASC))
    }
    @Test fun collectionCountsCombineCaseVariantsAndExcludeUnassignedTitles() {
        val groups = sortCollections(items + newest.copy(id = "none", collection = " "), CollectionSort.MOST_TITLES)
        assertEquals(listOf("marvel", "dc"), groups.map { it.key })
        assertEquals(2, groups.first().items.size)
        assertEquals(listOf("dc", "marvel"), sortCollections(items, CollectionSort.FEWEST_TITLES).map { it.key })
    }
    @Test fun collectionNamesSortBothWaysAndRecentAdditionsKeepStoredOrder() {
        assertEquals(listOf("marvel", "dc"), sortCollections(items, CollectionSort.RECENT).map { it.key })
        assertEquals(listOf("dc", "marvel"), sortCollections(items, CollectionSort.NAME_ASC).map { it.key })
        assertEquals(listOf("marvel", "dc"), sortCollections(items, CollectionSort.NAME_DESC).map { it.key })
    }
    @Test fun ratingTiesUseTitlesForConsistentOrdering() {
        val tied = items.map { it.copy(rating = 3) }
        assertEquals(listOf("Alpha", "beta", "zebra"), sortShelf(tied, ItemSort.RATING_DESC).map { it.title })
    }
}
