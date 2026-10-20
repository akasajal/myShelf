package com.myshelf.app.data.cover

import java.io.ByteArrayInputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class CoverInputTest {
    @Test fun trimsHttpsImageLinksAndPreservesSignedQueries() {
        val uri = CoverInput.imageLink("  https://example.com/cover.jpg?token=a%2Bb  ")
        assertEquals("https://example.com/cover.jpg?token=a%2Bb", uri.toString())
    }
    @Test fun rejectsWebPagesWithoutAHostAndNonHttpsSources() {
        listOf("", "example.com/cover.jpg", "http://example.com/cover.jpg", "file:///private/image.jpg",
            "content://images/1", "https:///cover.jpg", "https://user:password@example.com/cover.jpg").forEach {
            assertThrows(IllegalArgumentException::class.java) { CoverInput.imageLink(it) }
        }
    }
    @Test fun boundedImageReadPreservesBytes() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        assertArrayEquals(bytes, CoverInput.read(ByteArrayInputStream(bytes)))
    }
    @Test fun rejectsEmptyAndOversizedImages() {
        assertThrows(IllegalArgumentException::class.java) { CoverInput.read(byteArrayOf().inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { CoverInput.read(ByteArray(CoverInput.MAX_BYTES + 1).inputStream()) }
    }
    @Test fun cancellationStopsImageRead() {
        assertThrows(CancellationException::class.java) {
            CoverInput.read(byteArrayOf(1, 2).inputStream()) { throw CancellationException() }
        }
    }
}
