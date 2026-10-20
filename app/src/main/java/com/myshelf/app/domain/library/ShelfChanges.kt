package com.myshelf.app.domain.library

import com.myshelf.app.core.model.ShelfItem
import java.text.Normalizer
import java.util.Locale

private fun titleKey(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

fun findDuplicate(items: List<ShelfItem>, candidate: ShelfItem): ShelfItem? = items.firstOrNull {
    it.id != candidate.id && it.type == candidate.type && titleKey(it.title) == titleKey(candidate.title)
}


data class RemovedTitle(val item: ShelfItem, val index: Int)

fun restoreTitle(items: List<ShelfItem>, removed: RemovedTitle): List<ShelfItem> =
    if (items.any { it.id == removed.item.id }) items
    else items.toMutableList().apply { add(removed.index.coerceIn(0, size), removed.item) }
