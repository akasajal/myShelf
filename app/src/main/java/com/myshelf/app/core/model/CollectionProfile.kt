package com.myshelf.app.core.model

import java.util.Locale
import java.util.UUID

enum class CollectionIcon(val label: String) {
    SHELF("Shelf"), MOVIE("Movie"), TV("Television"), BOOK("Book"), LIBRARY("Library"),
    STAR("Star"), HEART("Heart"), WORLD("World"), COMPASS("Compass"), MAGIC("Magic"),
    SCIENCE("Science"), ROCKET("Rocket"), CASTLE("Castle"), GAMES("Games"), MUSIC("Music"), COMIC("Comic")
}

data class CollectionProfile(
    val name: String,
    val description: String = "",
    val icon: CollectionIcon = CollectionIcon.SHELF,
    val id: String = UUID.nameUUIDFromBytes("myshelf:collection:${collectionKey(name)}".toByteArray(Charsets.UTF_8)).toString()
)


fun collectionKey(name: String): String = name.trim().lowercase(Locale.ROOT)
