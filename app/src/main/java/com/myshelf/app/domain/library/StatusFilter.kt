package com.myshelf.app.domain.library

import com.myshelf.app.core.model.Progress
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.seasons.isCaughtUp
import com.myshelf.app.domain.seasons.seasonProgress

enum class StatusFilter(val label: String, private val progress: Progress?) {
    PLANNED("On my list", Progress.PLANNED), ACTIVE("In progress", Progress.ACTIVE),
    CAUGHT_UP("Caught up", null), FINISHED("Finished", Progress.FINISHED),
    PAUSED("On hold", Progress.PAUSED), DROPPED("Dropped", Progress.DROPPED);

    fun matches(item: ShelfItem): Boolean = when (this) {
        CAUGHT_UP -> item.isCaughtUp
        ACTIVE -> !item.isCaughtUp && item.seasonProgress() == Progress.ACTIVE
        else -> item.seasonProgress() == progress
    }
}
