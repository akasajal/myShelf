package com.myshelf.app

import java.text.Normalizer
import java.util.Locale

enum class CardDetails(val label: String) { COMFORTABLE("Comfortable"), COMPACT("Compact") }
enum class ImportMode(val label: String) {
    SKIP_EXISTING("Skip existing"), UPDATE_MATCHING("Update matching"), REPLACE("Replace shelf")
}

private fun titleKey(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

fun findDuplicate(items: List<ShelfItem>, candidate: ShelfItem): ShelfItem? = items.firstOrNull {
    it.id != candidate.id && it.type == candidate.type && titleKey(it.title) == titleKey(candidate.title)
}

data class ImportPreview(val added: List<ShelfItem>, val matching: List<ShelfItem>, val removed: List<ShelfItem>)

fun previewChanges(existing: List<ShelfItem>, incoming: List<ShelfItem>): ImportPreview {
    val existingIds = existing.map { it.id }.toSet()
    val incomingIds = incoming.map { it.id }.toSet()
    return ImportPreview(incoming.filterNot { it.id in existingIds }, incoming.filter { it.id in existingIds },
        existing.filterNot { it.id in incomingIds })
}

fun mergeShelf(existing: List<ShelfItem>, incoming: List<ShelfItem>, mode: ImportMode): List<ShelfItem> {
    if (mode == ImportMode.REPLACE) return incoming
    val replacements = incoming.associateBy { it.id }
    val existingIds = existing.map { it.id }.toSet()
    val kept = existing.map {
        if (mode == ImportMode.UPDATE_MATCHING) replacements[it.id] ?: it else it
    }
    return kept + incoming.filterNot { it.id in existingIds }
}

data class RemovedTitle(val item: ShelfItem, val index: Int)

fun restoreTitle(items: List<ShelfItem>, removed: RemovedTitle): List<ShelfItem> =
    if (items.any { it.id == removed.item.id }) items
    else items.toMutableList().apply { add(removed.index.coerceIn(0, size), removed.item) }
