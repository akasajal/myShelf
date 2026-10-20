package com.myshelf.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

@Composable
fun HomeScreen(items: List<ShelfItem>, loadError: String?, onOpen: (ShelfItem) -> Unit, onLongPress: (ShelfItem) -> Unit,
    onViewAll: (HomeSection) -> Unit, modifier: Modifier = Modifier) {
    val content = remember(items) { homeContent(items) }
    val continueScroll = rememberLazyListState()
    val recentScroll = rememberLazyListState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sideBySide = maxHeight < 440.dp && maxWidth > maxHeight
        val bannerHeight = (maxHeight * .23f).coerceIn(60.dp, 176.dp)
        val bottomSpace = if (maxHeight < 440.dp) 64.dp else 76.dp
        Column(Modifier.fillMaxSize().padding(bottom = bottomSpace), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = bannerHeight), contentAlignment = Alignment.Center) {
                HomeWelcomeDoodle(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (bannerHeight < 100.dp) 4.dp else 10.dp)) {
                    Text(if (items.isEmpty()) "Welcome to My Shelf" else "Welcome back", Modifier.fillMaxWidth(),
                        style = if (bannerHeight < 100.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center)
                    Text("Your stories, all in one place.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            if (loadError != null) Text(loadError, Modifier.padding(horizontal = 22.dp), color = MaterialTheme.colorScheme.error)
            if (sideBySide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeCarousel("Continue", content.continuing, "Nothing in progress", continueScroll, Modifier.weight(1f),
                    onOpen, onLongPress, { onViewAll(HomeSection.CONTINUE) })
                HomeCarousel("Recently added", content.recent, "No titles yet", recentScroll, Modifier.weight(1f),
                    onOpen, onLongPress, { onViewAll(HomeSection.RECENT) })
            } else {
                HomeCarousel("Continue", content.continuing, "Nothing in progress", continueScroll, Modifier.weight(1f),
                    onOpen, onLongPress, { onViewAll(HomeSection.CONTINUE) })
                HomeCarousel("Recently added", content.recent, "No titles yet", recentScroll, Modifier.weight(1f),
                    onOpen, onLongPress, { onViewAll(HomeSection.RECENT) })
            }
        }
    }
}

@Composable
private fun HomeCarousel(title: String, titles: List<ShelfItem>, emptyText: String, state: LazyListState,
    modifier: Modifier, onOpen: (ShelfItem) -> Unit, onLongPress: (ShelfItem) -> Unit, onViewAll: () -> Unit) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 22.dp), style = MaterialTheme.typography.titleMedium)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val lines = if (maxHeight < 170.dp) 1 else 2
            val titleHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() } * lines + 8.dp
            val posterHeight = (maxHeight - titleHeight).coerceIn(0.dp, 200.dp)
            val tileWidth = (posterHeight * .7f).coerceIn(64.dp, 140.dp)
            LazyRow(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (titles.isEmpty()) item(key = "empty", contentType = "empty") { HomeEmptyTile(emptyText, tileWidth, posterHeight) }
                items(titles, key = { "title:${it.id}" }, contentType = { "title" }) {
                    HomeTitleTile(it, tileWidth, posterHeight, lines, onClick = { onOpen(it) }, onLongClick = { onLongPress(it) })
                }
                item(key = "view-all", contentType = "view-all") { HomeViewAllTile("View all $title", tileWidth, posterHeight, onViewAll) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTitleTile(item: ShelfItem, width: Dp, height: Dp, lines: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.width(width).combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Title actions")) {
        Card(shape = RoundedCornerShape(12.dp)) { Cover(item, Modifier.fillMaxWidth().height(height)) }
        Text(item.title, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, minLines = lines, maxLines = lines,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeViewAllTile(description: String, width: Dp, height: Dp, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(width).height(height).semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(if (height < 100.dp) 20.dp else 30.dp))
            Spacer(Modifier.height(6.dp))
            Text("View all", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HomeEmptyTile(message: String, width: Dp, height: Dp) {
    Surface(Modifier.width(width).height(height), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
