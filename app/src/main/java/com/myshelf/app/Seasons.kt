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
    val previous = seasons.find { it.number == season.number }
    val resumed = previous != null && (previous.watched != season.watched || previous.progress != season.progress)
    val updated = copy(seasons = seasons.map { if (it.number == season.number) season else it },
        seriesDropped = seriesDropped && !resumed)
    return updated.copy(progress = updated.seasonProgress())
}

val MediaType.supportsSeasons: Boolean get() = this == MediaType.SERIES || this == MediaType.TV

fun ShelfItem.seasonProgress(): Progress {
    if (type.supportsSeasons && seriesDropped) return Progress.DROPPED
    if (!type.supportsSeasons || seasons.isEmpty()) return progress
    return when {
        seasons.all { it.progress == Progress.FINISHED } -> if (seriesEnded) Progress.FINISHED else Progress.ACTIVE
        seasons.any { it.progress == Progress.ACTIVE } -> Progress.ACTIVE
        seasons.any { it.progress == Progress.PAUSED } -> Progress.PAUSED
        seasons.any { it.progress == Progress.DROPPED } -> Progress.DROPPED
        seasons.any { it.progress == Progress.FINISHED } -> Progress.ACTIVE
        else -> Progress.PLANNED
    }
}

val ShelfItem.isCaughtUp: Boolean get() = type.supportsSeasons && seasons.isNotEmpty() && !seriesEnded && !seriesDropped && seasons.all { it.progress == Progress.FINISHED }

val ShelfItem.statusLabel: String get() = if (isCaughtUp) "Caught up" else seasonProgress().label

val ShelfItem.seasonSummary: String? get() = if (type.supportsSeasons && seasons.isNotEmpty())
    "${seasons.count { it.progress == Progress.FINISHED }}/${seasons.size} seasons finished" else null
