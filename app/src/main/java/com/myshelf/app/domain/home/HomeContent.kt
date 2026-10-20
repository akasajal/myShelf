package com.myshelf.app.domain.home

import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.library.StatusFilter

enum class HomeSection { CONTINUE, RECENT }

data class HomeContent(val continuing: List<ShelfItem>, val recent: List<ShelfItem>)

fun homeContent(items: List<ShelfItem>): HomeContent = HomeContent(
    continuing = items.asSequence().filter { StatusFilter.ACTIVE.matches(it) }.take(4).toList(),
    recent = items.take(6)
)
