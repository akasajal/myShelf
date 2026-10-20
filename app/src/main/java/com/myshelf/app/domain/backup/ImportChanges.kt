package com.myshelf.app.domain.backup

import com.myshelf.app.core.model.ShelfItem

enum class ImportMode(val label: String) {
    SKIP_EXISTING("Skip existing"), UPDATE_MATCHING("Update matching"), REPLACE("Replace shelf")
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
