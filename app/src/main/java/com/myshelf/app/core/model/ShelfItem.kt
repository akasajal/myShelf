package com.myshelf.app.core.model

import java.util.UUID

enum class MediaType(val label: String, val singular: String) {
    MOVIE("Movies", "Movie"), SERIES("Series", "Series"), TV("TV shows", "TV show"),
    COMIC("Comics", "Comic"), BOOK("Books", "Book"), OTHER("Other", "Other")
}
enum class Progress(val label: String) {
    PLANNED("On my list"), ACTIVE("In progress"), FINISHED("Finished"), PAUSED("On hold"), DROPPED("Dropped")
}
data class ShelfItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "", val type: MediaType = MediaType.MOVIE,
    val genre: String = "", val collection: String = "", val cover: String = "",
    val year: String = "", val creator: String = "", val rating: Int = 0,
    val progress: Progress = Progress.PLANNED, val notes: String = "",
    val seasons: List<Season> = emptyList(), val seriesEnded: Boolean = false, val seriesDropped: Boolean = false
)
