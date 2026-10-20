package com.myshelf.app

import org.junit.Assert.*
import org.junit.Test

class HomeContentTest {
    @Test fun continueIsLimitedToFourActiveTitlesInSavedOrder() {
        val titles = (1..8).map { ShelfItem(id = "$it", title = "Title $it", progress = Progress.ACTIVE) }
        val content = homeContent(titles)
        assertEquals(titles.take(4), content.continuing)
        assertEquals(titles.take(6), content.recent)
        assertEquals(8, titles.size)
    }

    @Test fun continueExcludesFinishedPausedDroppedPlannedAndCaughtUpTitles() {
        val ordinary = Progress.entries.map { ShelfItem(id = it.name, title = it.label, progress = it) }
        val caughtUp = ShelfItem(title = "Caught up", type = MediaType.SERIES, seasons = listOf(Season(1, Progress.FINISHED)))
        val droppedSeries = caughtUp.copy(title = "Dropped series", seriesDropped = true)
        val activeSeries = caughtUp.copy(title = "Next season", seasons = caughtUp.seasons + Season(2))
        val titles = ordinary + listOf(caughtUp, droppedSeries, activeSeries)
        val content = homeContent(titles)
        assertEquals(listOf(ordinary.single { it.progress == Progress.ACTIVE }, activeSeries), content.continuing)
        assertEquals(titles.take(6), content.recent)
    }

    @Test fun emptyAndSmallShelvesDoNotInventPreviewTitles() {
        assertTrue(homeContent(emptyList()).continuing.isEmpty())
        assertTrue(homeContent(emptyList()).recent.isEmpty())
        val item = ShelfItem(title = "A title", progress = Progress.ACTIVE)
        assertEquals(listOf(item), homeContent(listOf(item)).continuing)
        assertEquals(listOf(item), homeContent(listOf(item)).recent)
        assertTrue(homeContent(listOf(item.copy(progress = Progress.FINISHED))).continuing.isEmpty())
    }
}
