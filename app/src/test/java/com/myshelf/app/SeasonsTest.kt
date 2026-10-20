package com.myshelf.app

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class SeasonsTest {
    private val series = ShelfItem(title = "Loki", type = MediaType.SERIES,
        seasons = listOf(Season(1, Progress.FINISHED, 4, 6, 6), Season(2, episodes = 6)))

    @Test fun laterUnwatchedSeasonKeepsOneSeriesInProgress() {
        assertEquals(Progress.ACTIVE, series.seasonProgress())
        assertEquals("In progress", series.statusLabel)
        assertEquals("1/2 seasons finished", series.seasonSummary)
    }

    @Test fun completedSeasonsAreCaughtUpUntilSeriesEnds() {
        val caughtUp = series.copy(seasons = series.seasons.map { it.copy(progress = Progress.FINISHED, watched = it.episodes) })
        assertEquals("Caught up", caughtUp.statusLabel)
        assertEquals(Progress.ACTIVE, caughtUp.seasonProgress())
        assertEquals("Finished", caughtUp.copy(seriesEnded = true).statusLabel)
        assertEquals(Progress.FINISHED, caughtUp.copy(seriesEnded = true).seasonProgress())
        assertEquals("In progress", caughtUp.copy(seasons = caughtUp.seasons + Season(3)).statusLabel)
    }

    @Test fun plannedPausedAndUntrackedTitlesRetainTheirStatuses() {
        assertEquals(Progress.PLANNED, series.copy(seasons = listOf(Season(1))).seasonProgress())
        assertEquals(Progress.PAUSED, series.copy(seasons = listOf(Season(1, Progress.PAUSED))).seasonProgress())
        val untracked = series.copy(seasons = emptyList(), progress = Progress.FINISHED)
        assertEquals("Finished", untracked.statusLabel)
        assertNull(untracked.seasonSummary)
        assertNull(series.copy(type = MediaType.MOVIE).seasonSummary)
        assertTrue(MediaType.TV.supportsSeasons)
    }

    @Test fun seasonsRatingsEpisodesAndEndingSurvivePortableBackup() {
        val item = series.copy(seriesEnded = true, progress = series.seasonProgress())
        val bytes = ByteArrayOutputStream()
        ShelfBackup.write(bytes, listOf(item)) { null }
        val imported = ShelfBackup.read(bytes.toByteArray().inputStream())
        assertEquals(listOf(item), imported.items)
        assertEquals(1, imported.items.size)
    }

    @Test fun oldBackupsWithoutSeasonFieldsStillLoad() {
        val legacy = ShelfJson.encode(listOf(series.copy(seasons = emptyList())))
        legacy.getJSONObject(0).remove("seasons")
        legacy.getJSONObject(0).remove("seriesEnded")
        val restored = ShelfJson.decode(legacy).single()
        assertTrue(restored.seasons.isEmpty())
        assertFalse(restored.seriesEnded)
        assertEquals(series.title, restored.title)
    }

    @Test fun rejectsDuplicateSeasonNumbersAndInvalidEpisodeData() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfJson.decodeSeasons(ShelfJson.encodeSeasons(listOf(Season(1), Season(1))))
        }
        listOf(Season(0), Season(1, rating = 6), Season(1, watched = -1),
            Season(1, watched = 7, episodes = 6), Season(1, Progress.FINISHED, watched = 2, episodes = 6)).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                ShelfJson.decodeSeasons(ShelfJson.encodeSeasons(listOf(invalid)))
            }
        }
        val fractional = ShelfJson.encodeSeasons(listOf(Season(1)))
        fractional.getJSONObject(0).put("watched", 1.5)
        assertThrows(IllegalArgumentException::class.java) { ShelfJson.decodeSeasons(fractional) }
        assertThrows(IllegalArgumentException::class.java) {
            ShelfJson.decodeSeasons(JSONArray(List(501) { ShelfJson.encodeSeasons(listOf(Season(it + 1))).getJSONObject(0) }))
        }
    }
}
