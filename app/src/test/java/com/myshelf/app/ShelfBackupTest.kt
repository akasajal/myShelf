package com.myshelf.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ShelfBackupTest {
    private val title = ShelfItem(id = "test-id", title = "A story", type = MediaType.BOOK,
        genre = "Romance", collection = "Favorites", cover = "content://device/image/3",
        year = "2024", creator = "An author", rating = 4, progress = Progress.ACTIVE,
        notes = "Some thoughts\nwith a new line and 日本語.")

    @Test fun portableBackupPreservesAllMetadataAndCoverBytes() {
        val output = ByteArrayOutputStream()
        val bytes = byteArrayOf(1, 2, 3, 4)
        val result = ShelfBackup.write(output, listOf(title)) { ByteArrayInputStream(bytes) }
        val restored = ShelfBackup.read(output.toByteArray().inputStream())
        assertEquals(ExportResult(1, 0), result)
        assertEquals(title.copy(cover = "covers/0.img"), restored.items.single())
        assertArrayEquals(bytes, restored.covers.getValue("covers/0.img"))
        assertFalse(restored.items.single().cover.startsWith("content:"))
    }

    @Test fun unavailableCoverIsReportedAndMetadataRemainsRestorable() {
        val output = ByteArrayOutputStream()
        val result = ShelfBackup.write(output, listOf(title)) { null }
        assertEquals(1, result.missingCovers)
        assertEquals(title.copy(cover = ""), ShelfBackup.read(output.toByteArray().inputStream()).items.single())
    }

    @Test fun emptyShelfRoundTrips() {
        val output = ByteArrayOutputStream()
        ShelfBackup.write(output, emptyList()) { null }
        assertTrue(ShelfBackup.read(output.toByteArray().inputStream()).items.isEmpty())
    }

    @Test fun rejectsCoverPathsOutsideArchive() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfBackup.read(archive(manifest(listOf(title.copy(cover = "file:///private/image")))).inputStream())
        }
    }

    @Test fun rejectsPathTraversalEntry() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfBackup.read(archive(manifest(emptyList()), "../escape.img").inputStream())
        }
    }

    @Test fun rejectsMissingCoverBeforeApplyingAnything() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfBackup.read(archive(manifest(listOf(title.copy(cover = "covers/0.img")))).inputStream())
        }
    }

    @Test fun rejectsDuplicateIdsAndInvalidRating() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfJson.decode(ShelfJson.encode(listOf(title, title)))
        }
        val data = ShelfJson.encode(listOf(title))
        data.getJSONObject(0).put("rating", 6)
        assertThrows(IllegalArgumentException::class.java) { ShelfJson.decode(data) }
    }

    @Test fun rejectsNewerBackupVersion() {
        val json = JSONObject(manifest(emptyList())).put("version", 2).toString()
        assertThrows(IllegalArgumentException::class.java) { ShelfBackup.read(archive(json).inputStream()) }
    }

    @Test fun rejectsMissingManifest() {
        assertThrows(IllegalArgumentException::class.java) { ShelfBackup.read(byteArrayOf(0, 1).inputStream()) }
    }

    @Test fun rejectsOversizedCoverWhileExporting() {
        assertThrows(IllegalArgumentException::class.java) {
            ShelfBackup.write(ByteArrayOutputStream(), listOf(title)) { ByteArray(8 * 1024 * 1024 + 1).inputStream() }
        }
    }

    private fun manifest(items: List<ShelfItem>): String = JSONObject()
        .put("format", "myshelf").put("version", 1).put("items", ShelfJson.encode(items)).toString()

    private fun archive(manifest: String, extraEntry: String? = null): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("shelf.json")); zip.write(manifest.toByteArray()); zip.closeEntry()
            extraEntry?.let { zip.putNextEntry(ZipEntry(it)); zip.write(byteArrayOf(1)); zip.closeEntry() }
        }
        return output.toByteArray()
    }
}
