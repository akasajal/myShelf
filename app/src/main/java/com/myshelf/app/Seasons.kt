package com.myshelf.app

data class Season(
    val number: Int,
    val progress: Progress = Progress.PLANNED,
    val rating: Int = 0,
    val watched: Int = 0,
    val episodes: Int = 0
)

val MediaType.supportsSeasons: Boolean get() = this == MediaType.SERIES || this == MediaType.TV

fun ShelfItem.seasonProgress(): Progress {
    if (!type.supportsSeasons || seasons.isEmpty()) return progress
    return when {
        seasons.all { it.progress == Progress.FINISHED } -> if (seriesEnded) Progress.FINISHED else Progress.ACTIVE
        seasons.any { it.progress == Progress.ACTIVE || it.progress == Progress.FINISHED } -> Progress.ACTIVE
        seasons.any { it.progress == Progress.PAUSED } -> Progress.PAUSED
        else -> Progress.PLANNED
    }
}

val ShelfItem.statusLabel: String get() =
    if (type.supportsSeasons && seasons.isNotEmpty() && !seriesEnded && seasons.all { it.progress == Progress.FINISHED })
        "Caught up" else seasonProgress().label

val ShelfItem.seasonSummary: String? get() = if (type.supportsSeasons && seasons.isNotEmpty())
    "${seasons.count { it.progress == Progress.FINISHED }}/${seasons.size} seasons finished" else null
