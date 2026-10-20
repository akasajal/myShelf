package com.myshelf.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(items: List<ShelfItem>, loadError: String?, onOpen: (ShelfItem) -> Unit, onLongPress: (ShelfItem) -> Unit,
    onViewAll: (HomeSection) -> Unit, modifier: Modifier = Modifier) {
    val content = remember(items) { homeContent(items) }
    val continueScroll = rememberLazyListState()
    val recentScroll = rememberLazyListState()
    LazyColumn(modifier.fillMaxSize(), state = rememberLazyListState(), contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "welcome", contentType = "heading") {
            Column(Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                Text(if (items.isEmpty()) "Welcome to My Shelf" else "Welcome back", Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("Your stories, all in one place.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        if (loadError != null) item(key = "error") {
            Text(loadError, Modifier.padding(horizontal = 22.dp), color = MaterialTheme.colorScheme.error)
        }
        item(key = "continue-heading", contentType = "heading") {
            Text("Continue", Modifier.padding(horizontal = 22.dp), style = MaterialTheme.typography.titleMedium)
        }
        item(key = "continue-row", contentType = "row") {
            LazyRow(state = continueScroll, contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (content.continuing.isEmpty()) item(key = "empty", contentType = "empty") { HomeEmptyTile("Nothing in progress") }
                items(content.continuing, key = { "title:${it.id}" }, contentType = { "title" }) {
                    HomeTitleTile(it, onClick = { onOpen(it) }, onLongClick = { onLongPress(it) })
                }
                item(key = "view-all", contentType = "view-all") { HomeViewAllTile("View all titles in progress") { onViewAll(HomeSection.CONTINUE) } }
            }
        }
        item(key = "recent-heading", contentType = "heading") {
            Text("Recently added", Modifier.padding(horizontal = 22.dp), style = MaterialTheme.typography.titleMedium)
        }
        item(key = "recent-row", contentType = "row") {
            LazyRow(state = recentScroll, contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (content.recent.isEmpty()) item(key = "empty", contentType = "empty") { HomeEmptyTile("No titles yet") }
                items(content.recent, key = { "title:${it.id}" }, contentType = { "title" }) {
                    HomeTitleTile(it, onClick = { onOpen(it) }, onLongClick = { onLongPress(it) })
                }
                item(key = "view-all", contentType = "view-all") { HomeViewAllTile("View all recently added titles") { onViewAll(HomeSection.RECENT) } }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTitleTile(item: ShelfItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.width(140.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Title actions")) {
        Card(shape = RoundedCornerShape(12.dp)) { Cover(item, Modifier.fillMaxWidth().height(200.dp)) }
        Text(item.title, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, minLines = 2, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeViewAllTile(description: String, onClick: () -> Unit) {
    Column(Modifier.width(140.dp)) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = description },
            shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(30.dp))
                Spacer(Modifier.height(10.dp))
                Text("View all", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HomeEmptyTile(message: String) {
    Surface(Modifier.width(140.dp).height(200.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeAddButton(onCard: () -> Unit, onCollection: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AddShelfButton(label = "Add card or collection") { expanded = !expanded }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("Card") }, leadingIcon = { Icon(Icons.Outlined.Style, null) }, onClick = { expanded = false; onCard() })
            DropdownMenuItem(text = { Text("Collection") }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { expanded = false; onCollection() })
        }
    }
}
