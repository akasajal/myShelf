package com.myshelf.app.features.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.CollectionSort
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.library.sortCollections
import com.myshelf.app.ui.components.EmptyShelf
import com.myshelf.app.ui.components.SortMenu
import com.myshelf.app.ui.components.vector

@Composable
fun CollectionsScreen(items: List<ShelfItem>, profiles: List<CollectionProfile>, sort: CollectionSort, listState: LazyListState,
    loadError: String?, canEdit: Boolean, onSort: (CollectionSort) -> Unit, onOpen: (String) -> Unit,
    onEdit: (String) -> Unit, modifier: Modifier = Modifier) {
    val groups = remember(items, profiles, sort) { sortCollections(items, sort, profiles) }
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("Collections", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 20.dp))
        loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${groups.size} collections", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            SortMenu(sort, CollectionSort.entries, { it.label }, onSort)
        }
        if (groups.isEmpty()) EmptyShelf("No collections yet", "Tap + to create a collection.", Modifier.weight(1f))
        else LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(groups, key = { it.key }, contentType = { "collection" }) { group ->
                Card(onClick = { onOpen(group.name) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(group.profile.icon.vector, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(group.name, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                            if (group.profile.description.isNotBlank()) Text(group.profile.description, maxLines = 2,
                                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${group.items.size} titles · ${group.items.map { it.type }.distinct().size} media types", fontSize = 12.sp)
                        }
                        IconButton(onClick = { onEdit(group.name) }, enabled = canEdit) { Icon(Icons.Outlined.Edit, "Customize ${group.name}") }
                    }
                }
            }
        }
    }
}
