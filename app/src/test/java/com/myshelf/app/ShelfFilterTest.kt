package com.myshelf.app

import org.junit.Assert.*
import org.junit.Test

class ShelfFilterTest {
    private val movie = ShelfItem(title = "Movie", type = MediaType.MOVIE, genre = "Romance", collection = "Marvel")
    private val book = ShelfItem(title = "Book", type = MediaType.BOOK, genre = "Romance", collection = "Marvel")
    @Test fun genreNeverCrossesMediaTypes() {
        assertEquals(listOf(book), filterShelf(listOf(movie, book), MediaType.BOOK, "Romance", null, ""))
        assertTrue(filterShelf(listOf(movie, book), null, "Romance", null, "").isEmpty())
    }
    @Test fun collectionsSpanMediaTypes() {
        assertEquals(2, filterShelf(listOf(movie, book), null, null, "marvel", "").size)
    }
    @Test fun filtersCombineAndSearchIgnoresCaseAndOuterSpaces() {
        assertEquals(listOf(movie), filterShelf(listOf(movie, book), MediaType.MOVIE, "Romance", "Marvel", " MOVIE "))
        assertTrue(filterShelf(listOf(movie, book), MediaType.BOOK, null, null, "Movie").isEmpty())
    }
    @Test fun statusCombinesWithMediaCollectionAndSearch() {
        val dropped = movie.copy(progress = Progress.DROPPED)
        val droppedBook = book.copy(progress = Progress.DROPPED)
        assertEquals(listOf(dropped), filterShelf(listOf(dropped, droppedBook, movie), MediaType.MOVIE,
            null, "Marvel", "movie", StatusFilter.DROPPED))
        assertEquals(listOf(movie), filterShelf(listOf(dropped, droppedBook, movie), null,
            null, null, "", StatusFilter.PLANNED))
        assertEquals(3, filterShelf(listOf(dropped, droppedBook, movie), null, null, null, "").size)
    }

    @Test fun statusFilteringUsesSeasonProgressAndSeparatesCaughtUpFromInProgress() {
        val caughtUp = ShelfItem(title = "Caught up", type = MediaType.SERIES,
            seasons = listOf(Season(1, Progress.FINISHED)))
        val active = caughtUp.copy(title = "Active", seasons = caughtUp.seasons + Season(2))
        val dropped = caughtUp.copy(title = "Dropped", seasons = caughtUp.seasons + Season(2, Progress.DROPPED))
        val finished = caughtUp.copy(title = "Ended", seriesEnded = true)
        val items = listOf(caughtUp, active, dropped, finished)
        assertEquals(listOf(caughtUp), filterShelf(items, null, null, null, "", StatusFilter.CAUGHT_UP))
        assertEquals(listOf(active), filterShelf(items, null, null, null, "", StatusFilter.ACTIVE))
        assertEquals(listOf(dropped), filterShelf(items, null, null, null, "", StatusFilter.DROPPED))
        assertEquals(listOf(finished), filterShelf(items, null, null, null, "", StatusFilter.FINISHED))
    }
}
