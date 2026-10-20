package com.myshelf.app.features.shelf

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myshelf.app.core.model.CardDetails
import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.ItemSort
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.library.StatusFilter
import com.myshelf.app.features.shelf.components.ShelfCard
import com.myshelf.app.ui.components.EmptyShelf
import com.myshelf.app.ui.components.SortMenu
import com.myshelf.app.ui.components.StatusFilterMenu
import com.myshelf.app.ui.components.vector

data class ShelfScreenState(
    val items: List<ShelfItem>, val hasTitles: Boolean, val query: String,
    val status: StatusFilter?, val order: ItemSort, val cardsPerRow: Int,
    val cardDetails: CardDetails, val loadError: String?
)

/** Shared title grid for My Shelf and the contents of a collection. */
@Composable
fun ShelfScreen(state: ShelfScreenState, gridState: LazyGridState, collection: CollectionProfile?, canEditCollection: Boolean,
    onEditCollection: () -> Unit, onQuery: (String) -> Unit, onStatus: (StatusFilter?) -> Unit,
    onSort: (ItemSort) -> Unit, onOpen: (ShelfItem) -> Unit, onLongPress: (ShelfItem) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = if (collection?.description.isNullOrBlank()) 20.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (collection != null) {
                Icon(collection.icon.vector, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
            }
            Text(collection?.name ?: "My Shelf", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface)
            if (collection != null) IconButton(onClick = onEditCollection, enabled = canEditCollection) {
                Icon(Icons.Outlined.Edit, "Customize collection")
            }
        }
        collection?.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, Modifier.padding(bottom = 16.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        state.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(state.query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Search shelf") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.items.size} titles", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            StatusFilterMenu(state.status, onStatus)
            SortMenu(state.order, ItemSort.entries, { it.label }, onSort)
        }
        if (state.items.isEmpty()) EmptyShelf(if (state.hasTitles) "No matches" else "No titles yet",
            if (state.hasTitles) "Try another filter." else "Add a title to get started.", Modifier.weight(1f))
        else LazyVerticalGrid(GridCells.Fixed(state.cardsPerRow), state = gridState,
            horizontalArrangement = Arrangement.spacedBy(if (state.cardsPerRow == 2) 16.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(state.items, key = { it.id }, contentType = { "title" }) { item ->
                ShelfCard(item, compact = state.cardsPerRow > 2, details = state.cardDetails,
                    onClick = { onOpen(item) }, onLongClick = { onLongPress(item) })
            }
        }
    }
}
