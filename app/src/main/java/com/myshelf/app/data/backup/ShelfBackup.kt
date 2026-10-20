package com.myshelf.app.data.backup

import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.data.local.CollectionJson
import com.myshelf.app.data.local.ShelfJson
import com.myshelf.app.domain.collections.validateCollectionProfiles
import com.myshelf.app.ui.components.Cover
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

data class ImportedShelf(val items: List<ShelfItem>, val covers: Map<String, ByteArray>, val collections: List<CollectionProfile> = emptyList())
data class ExportResult(val titles: Int, val missingCovers: Int)

/** Portable backup: cover references are archive keys, never device paths. */
object ShelfBackup {
    private const val MAX_ITEMS = 2000
    private const val MAX_MANIFEST = 4 * 1024 * 1024
    private const val MAX_COVER = 8 * 1024 * 1024
    private const val MAX_TOTAL = 64 * 1024 * 1024
    private val coverName = Regex("covers/[0-9]+\\.img")

    fun write(output: OutputStream, items: List<ShelfItem>, collections: List<CollectionProfile> = emptyList(),
        readCover: (String) -> InputStream?): ExportResult {
        require(items.size <= MAX_ITEMS) { "Backups support up to $MAX_ITEMS titles." }
        validateCollectionProfiles(collections)
        var missing = 0
        var total = 0
        val portable = mutableListOf<ShelfItem>()
        ZipOutputStream(output).use { zip ->
            items.forEachIndexed { index, item ->
                val bytes = if (item.cover.isBlank()) null else {
                    val stream = try { readCover(item.cover) } catch (_: java.io.IOException) { null }
                    catch (_: SecurityException) { null }
                    stream?.use { readLimited(it, MAX_COVER) }.also { if (it == null) missing++ }
                }
                val path = if (bytes == null) "" else "covers/$index.img"
                if (bytes != null) {
                    total += bytes.size
                    require(total <= MAX_TOTAL - MAX_MANIFEST) { "Cover images exceed the 60 MB backup limit." }
                    zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry()
                }
                portable += item.copy(cover = path)
            }
            val manifest = JSONObject().put("format", "myshelf").put("version", 1)
                .put("items", ShelfJson.encode(portable)).put("collections", CollectionJson.encode(collections)).toString(2).toByteArray(Charsets.UTF_8)
            require(manifest.size <= MAX_MANIFEST) { "Shelf details exceed the 4 MB backup limit." }
            zip.putNextEntry(ZipEntry("shelf.json")); zip.write(manifest); zip.closeEntry()
        }
        return ExportResult(items.size, missing)
    }

    fun read(input: InputStream): ImportedShelf {
        var manifest: ByteArray? = null
        val covers = mutableMapOf<String, ByteArray>()
        val names = mutableSetOf<String>()
        var total = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && (entry.name == "shelf.json" || coverName.matches(entry.name))) { "Unrecognized backup entry." }
                require(names.add(entry.name) && names.size <= MAX_ITEMS + 1) { "Duplicate or excessive backup entries." }
                val limit = if (entry.name == "shelf.json") MAX_MANIFEST else MAX_COVER
                val bytes = readLimited(zip, minOf(limit, MAX_TOTAL - total))
                total += bytes.size
                if (entry.name == "shelf.json") manifest = bytes else covers[entry.name] = bytes
                zip.closeEntry()
            }
        }
        val root = JSONObject(requireNotNull(manifest) { "This isn't a My Shelf backup." }.toString(Charsets.UTF_8))
        require(root.getString("format") == "myshelf" && root.get("version") == 1) { "Unsupported backup version." }
        val array = root.getJSONArray("items")
        require(array.length() <= MAX_ITEMS) { "Too many titles in backup." }
        val items = ShelfJson.decode(array)
        require(items.all { it.cover.isBlank() || (coverName.matches(it.cover) && covers.containsKey(it.cover)) }) { "A cover is missing or invalid." }
        val collections = if (root.has("collections")) CollectionJson.decode(root.getJSONArray("collections")) else emptyList()
        return ImportedShelf(items, covers, collections)
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(result.size() + count <= limit) { "Backup file or cover image is too large." }
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }
}
