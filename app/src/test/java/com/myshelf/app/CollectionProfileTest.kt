package com.myshelf.app

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class CollectionProfileTest {
    private val movie = ShelfItem(id = "movie", title = "Iron Man", collection = "MCU")
    private val book = ShelfItem(id = "book", title = "A book", type = MediaType.BOOK, collection = " mcu ")
    private val profile = CollectionProfile("MCU", "The connected universe.", CollectionIcon.ROCKET)

    @Test fun renameUpdatesEveryMemberAndPreservesIdentityAndOtherTitles() {
        val unrelated = movie.copy(id = "dc", collection = "DC")
        val next = customizeCollection(listOf(movie, book, unrelated), listOf(profile), "MCU",
            profile.copy(name = " Marvel ", description = " Favorites ", icon = CollectionIcon.STAR))
        assertEquals(listOf("Marvel", "Marvel", "DC"), next.items.map { it.collection })
        assertEquals(profile.id, next.profiles.single().id)
        assertEquals("Favorites", next.profiles.single().description)
        assertEquals(CollectionIcon.STAR, next.profiles.single().icon)
        assertEquals(unrelated, next.items.last())
    }

    @Test fun inferredCollectionsKeepTheSameIdentityWhenFirstCustomized() {
        assertEquals(CollectionProfile("mcu").id, CollectionProfile("MCU").id)
        val next = customizeCollection(listOf(movie), emptyList(), "MCU", profile.copy(name = "Marvel"))
        assertEquals(CollectionProfile("MCU").id, next.profiles.single().id)
    }

    @Test fun duplicateAndEmptyNamesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            customizeCollection(listOf(movie, movie.copy(collection = "DC")), listOf(profile), "MCU", profile.copy(name = " dc "))
        }
        assertThrows(IllegalArgumentException::class.java) {
            customizeCollection(listOf(movie), listOf(profile), "MCU", profile.copy(name = " "))
        }
        val caseChange = customizeCollection(listOf(movie), listOf(profile), "MCU", profile.copy(name = "mcu"))
        assertEquals("mcu", caseChange.items.single().collection)
    }

    @Test fun customIconsNamesAndDescriptionsSurvivePortableBackupsIncludingEmptyCollections() {
        val empty = CollectionProfile("Next up", "A future collection", CollectionIcon.COMPASS)
        val output = ByteArrayOutputStream()
        ShelfBackup.write(output, listOf(movie), listOf(profile, empty)) { null }
        val restored = ShelfBackup.read(output.toByteArray().inputStream())
        assertEquals(listOf(movie), restored.items)
        assertEquals(listOf(profile, empty), restored.collections)
    }

    @Test fun metadataCanBeUpdatedSkippedOrReplacedOnImport() {
        val updated = profile.copy(description = "New description", icon = CollectionIcon.MAGIC)
        val empty = CollectionProfile("Other", icon = CollectionIcon.BOOK)
        val skip = mergeCollectionData(listOf(movie), listOf(profile, empty), listOf(movie), listOf(updated), ImportMode.SKIP_EXISTING)
        assertEquals(listOf(profile, empty), skip.profiles)
        val update = mergeCollectionData(listOf(movie), listOf(profile, empty), listOf(movie), listOf(updated), ImportMode.UPDATE_MATCHING)
        assertEquals(listOf(updated, empty), update.profiles)
        val replace = mergeCollectionData(listOf(movie), listOf(profile), emptyList(), listOf(empty), ImportMode.REPLACE)
        assertTrue(replace.items.isEmpty())
        assertEquals(listOf(empty), replace.profiles)
        assertEquals(listOf(profile), mergeCollectionData(listOf(movie), listOf(profile), listOf(movie), emptyList(), ImportMode.UPDATE_MATCHING).profiles)
    }

    @Test fun importedRenamesFollowStableIdsForUnrelatedCollectionMembers() {
        val updated = profile.copy(name = "Marvel")
        val incoming = movie.copy(collection = "Marvel", notes = "Updated")
        val next = mergeCollectionData(listOf(movie, book), listOf(profile), listOf(incoming), listOf(updated), ImportMode.UPDATE_MATCHING)
        assertEquals(listOf("Marvel", "Marvel"), next.items.map { it.collection })
        assertEquals(updated, next.profiles.single())
        val inferred = mergeCollectionData(listOf(movie, book), emptyList(), listOf(incoming), listOf(updated), ImportMode.UPDATE_MATCHING)
        assertEquals(listOf("Marvel", "Marvel"), inferred.items.map { it.collection })
        val skipped = mergeCollectionData(listOf(book), listOf(profile), listOf(incoming), listOf(updated), ImportMode.SKIP_EXISTING)
        assertTrue(skipped.items.all { collectionKey(it.collection) == "mcu" })
    }

    @Test fun renameCollisionsOnImportCannotSilentlyMergeCollections() {
        val other = movie.copy(id = "other", collection = "Marvel")
        assertThrows(IllegalArgumentException::class.java) {
            mergeCollectionData(listOf(movie, other), listOf(profile), emptyList(), listOf(profile.copy(name = "Marvel")), ImportMode.UPDATE_MATCHING)
        }
    }

    @Test fun skipDoesNotRenameExistingInferredCollections() {
        val incomingProfile = profile.copy(name = "Marvel")
        val incoming = movie.copy(id = "new", collection = "Marvel")
        val next = mergeCollectionData(listOf(movie), emptyList(), listOf(incoming), listOf(incomingProfile), ImportMode.SKIP_EXISTING)
        assertEquals(movie, next.items.first())
        assertEquals("MCU", next.items.last().collection)
        assertEquals(CollectionProfile("MCU"), next.profiles.single())
    }

    @Test fun groupsShowCustomizationAndRetainEmptyCollectionsButStatusFiltersCanHideThem() {
        val empty = CollectionProfile("Empty", "Keep me", CollectionIcon.HEART)
        val groups = sortCollections(listOf(movie), CollectionSort.NAME_ASC, listOf(profile, empty))
        assertEquals(listOf("Empty", "MCU"), groups.map { it.name })
        assertEquals(CollectionIcon.ROCKET, groups.last().profile.icon)
        assertEquals(profile.description, groups.last().profile.description)
        assertEquals(0, groups.first().items.size)
        assertTrue(sortCollections(emptyList(), CollectionSort.RECENT, listOf(profile, empty), includeEmpty = false).isEmpty())
    }

    @Test fun malformedMetadataIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { CollectionJson.decode(CollectionJson.encode(listOf(profile, profile))) }
        assertThrows(IllegalArgumentException::class.java) { CollectionJson.decode(CollectionJson.encode(listOf(profile.copy(description = "x".repeat(1001))))) }
        val invalid = CollectionJson.encode(listOf(profile))
        invalid.getJSONObject(0).put("icon", "UNKNOWN")
        assertThrows(IllegalArgumentException::class.java) { CollectionJson.decode(invalid) }
        assertTrue(CollectionJson.decode(JSONArray()).isEmpty())
    }
}
