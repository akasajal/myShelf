package com.myshelf.app

data class Season(
    val number: Int,
    val progress: Progress = Progress.PLANNED,
    val rating: Int = 0,
    val watched: Int = 0,
    val episodes: Int = 0,
    val name: String = ""
)

val Season.displayName: String get() = "Season $number" + if (name.isBlank()) "" else " · $name"

fun Season.withWatched(count: Int): Season {
    val next = count.coerceIn(0, if (episodes > 0) episodes else 9999)
    return copy(watched = next, progress = when {
        episodes > 0 && next == episodes -> Progress.FINISHED
        next > 0 -> Progress.ACTIVE
        else -> Progress.PLANNED
    })
}

fun Season.withProgress(value: Progress): Season = copy(progress = value,
    watched = if (value == Progress.FINISHED && episodes > 0) episodes else watched)

fun ShelfItem.withSeason(season: Season): ShelfItem {
    val updated = copy(seasons = seasons.map { if (it.number == season.number) season else it })
    return updated.copy(progress = updated.seasonProgress())
}

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
