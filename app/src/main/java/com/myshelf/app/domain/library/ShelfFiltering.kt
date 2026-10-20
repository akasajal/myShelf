package com.myshelf.app.domain.library

import com.myshelf.app.core.model.MediaType
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.core.model.collectionKey

fun filterShelf(items: List<ShelfItem>, type: MediaType?, genre: String?, collection: String?, query: String,
    status: StatusFilter? = null): List<ShelfItem> =
    items.filter { item ->
        (type == null || item.type == type) &&
            (genre == null || (type != null && item.type == type && item.genre.equals(genre, true))) &&
            (collection == null || collectionKey(item.collection) == collectionKey(collection)) &&
            (status == null || status.matches(item)) &&
            (query.isBlank() || listOf(item.title, item.creator, item.collection).any { it.contains(query.trim(), true) })
    }
