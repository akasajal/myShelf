package com.myshelf.app.core.model

data class ShelfCollection(val key: String, val name: String, val items: List<ShelfItem>, val profile: CollectionProfile = CollectionProfile(name))
