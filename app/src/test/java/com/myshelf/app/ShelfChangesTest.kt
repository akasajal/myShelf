package com.myshelf.app

import org.junit.Assert.*
import org.junit.Test

class ShelfChangesTest {
    private val old = ShelfItem(id = "loki", title = "Loki", type = MediaType.SERIES, rating = 4,
        cover = "old.jpg", notes = "Saved note", seasons = listOf(Season(1)))
    private val unrelated = ShelfItem(id = "book", title = "A book", type = MediaType.BOOK)
    private val updated = old.copy(rating = 5, cover = "new.jpg", notes = "Backup note",
        seasons = listOf(Season(1, Progress.FINISHED, 5, 6, 6, "Glorious Purpose")))
    private val added = ShelfItem(id = "new", title = "A new movie")

    @Test fun updateMatchingReplacesAllDetailsAndPreservesUnrelatedTitlesAndOrder() {
        val actual = mergeShelf(listOf(old, unrelated), listOf(updated, added), ImportMode.UPDATE_MATCHING)
        assertEquals(listOf(updated, unrelated, added), actual)
        assertEquals("Backup note", actual.first().notes)
        assertEquals("Glorious Purpose", actual.first().seasons.single().name)
    }

    @Test fun skipExistingPreservesPersonalDetails() {
        assertEquals(listOf(old, unrelated, added),
            mergeShelf(listOf(old, unrelated), listOf(updated, added), ImportMode.SKIP_EXISTING))
    }

    @Test fun replaceUsesExactlyTheBackupIncludingAnEmptyShelf() {
        assertEquals(listOf(updated, added), mergeShelf(listOf(old, unrelated), listOf(updated, added), ImportMode.REPLACE))
        assertTrue(mergeShelf(listOf(old), emptyList(), ImportMode.REPLACE).isEmpty())
        assertEquals(listOf(old), mergeShelf(listOf(old), emptyList(), ImportMode.UPDATE_MATCHING))
    }

    @Test fun previewCountsAndImportDecisionsUseIdsRatherThanTitleCollisions() {
        val sameName = old.copy(id = "another-loki")
        val preview = previewChanges(listOf(old, unrelated), listOf(updated, added, sameName))
        assertEquals(listOf(added, sameName), preview.added)
        assertEquals(listOf(updated), preview.matching)
        assertEquals(listOf(unrelated), preview.removed)
        val result = mergeShelf(listOf(old, unrelated), listOf(updated, added, sameName), ImportMode.UPDATE_MATCHING)
        assertEquals(4, result.size)
    }

    @Test fun duplicateWarningIgnoresCaseSpacingAndPunctuationButRespectsTypeAndId() {
        val movie = ShelfItem(id = "movie", title = "Spider-Man: Homecoming")
        assertEquals(movie, findDuplicate(listOf(movie), movie.copy(id = "draft", title = "  SPIDER MAN Homecoming ")))
        assertNull(findDuplicate(listOf(movie), movie.copy(title = "Spider Man Homecoming")))
        assertNull(findDuplicate(listOf(movie), movie.copy(id = "draft", type = MediaType.BOOK)))
        assertNull(findDuplicate(listOf(movie), movie.copy(id = "draft", title = "Far From Home")))
    }

    @Test fun undoRestoresPositionWithoutOverwritingNewerItems() {
        assertEquals(listOf(unrelated, old, added), restoreTitle(listOf(unrelated, added), RemovedTitle(old, 1)))
        assertEquals(listOf(updated), restoreTitle(listOf(updated), RemovedTitle(old, 0)))
        assertEquals(listOf(added, old), restoreTitle(listOf(added), RemovedTitle(old, 99)))
    }

    @Test fun browsingContextsRoundTripWithoutLeakingQueriesOrFiltersBetweenScreens() {
        val contexts = mapOf("shelf" to BrowsingContext("Dune", "BOOK", "DROPPED"),
            "collection:mcu" to BrowsingContext("Loki", "SERIES", "ACTIVE"), "collection:dc" to BrowsingContext())
        val restored = decodeBrowsingContexts(encodeBrowsingContexts(contexts))
        assertEquals(contexts, restored)
        assertEquals(BrowsingContext(), restored.getValue("collection:dc"))
        assertTrue(decodeBrowsingContexts(encodeBrowsingContexts(emptyMap())).isEmpty())
        assertEquals(BrowsingContext("Dune", "BOOK"), decodeBrowsingContexts("""{"shelf":{"query":"Dune","media":"BOOK"}}""").getValue("shelf"))
    }
}
