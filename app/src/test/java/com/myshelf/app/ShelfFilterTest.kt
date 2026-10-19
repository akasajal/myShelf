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
}
