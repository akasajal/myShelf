package com.myshelf.app.features.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myshelf.app.core.model.Progress
import com.myshelf.app.core.model.Season
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.seasons.displayName
import com.myshelf.app.domain.seasons.seasonSummary
import com.myshelf.app.domain.seasons.statusLabel
import com.myshelf.app.domain.seasons.supportsSeasons
import com.myshelf.app.domain.seasons.withProgress
import com.myshelf.app.domain.seasons.withSeason
import com.myshelf.app.domain.seasons.withWatched
import com.myshelf.app.features.seasons.SeasonDialog
import com.myshelf.app.ui.components.Cover

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TitleDetails(item: ShelfItem, collections: List<String>, onBack: () -> Unit, onEdit: () -> Unit,
    onSave: (ShelfItem) -> Unit, onRemove: () -> Unit) {
    var action by rememberSaveable(item.id) { mutableStateOf<TitleAction?>(null) }
    var seasonNumber by rememberSaveable(item.id) { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val seasons = remember(item.seasons) { item.seasons.sortedBy { it.number } }
    BackHandler(onBack = onBack)
    action?.let { selected -> key(item.id, selected) {
        TitleActionDialog(item, selected, collections, onSave) { action = null }
    } }
    seasonNumber?.let { number ->
        val existing = item.seasons.find { it.number == number }
        val initial = existing ?: Season((1..999).first { n -> item.seasons.none { it.number == n } })
        SeasonDialog(initial, existing != null, item.seasons.filter { it.number != number }.map { it.number }.toSet(),
            onDismiss = { seasonNumber = null },
            onSave = { season ->
                onSave(item.copy(seasons = (item.seasons.filterNot { it.number == number } + season).sortedBy { it.number }))
                seasonNumber = null
            },
            onRemove = { onSave(item.copy(seasons = item.seasons.filterNot { it.number == number })); seasonNumber = null })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(item.type.singular) }, navigationIcon = {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = {
            IconButton(onEdit) { Icon(Icons.Outlined.Edit, "Edit title") }
            IconButton(onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove title", tint = MaterialTheme.colorScheme.error) }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "artwork", contentType = "artwork") {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val artworkHeight = (maxWidth / .85f).coerceIn(260.dp, 420.dp)
                        Box(Modifier.fillMaxWidth().height(artworkHeight)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Cover(item, Modifier.fillMaxSize().blur(24.dp).alpha(.4f).clearAndSetSemantics {}, backdrop = true, placeholder = {})
                            Cover(item, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, highQuality = true)
                        }
                    }
                }
            }
            item(key = "information", contentType = "information") {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.title, style = MaterialTheme.typography.headlineSmall)
                        val metadata = listOf(item.year, item.genre).filter { it.isNotBlank() }
                        if (metadata.isNotEmpty()) Text(metadata.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.creator.isNotBlank()) Text(item.creator, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { action = TitleAction.STATUS }, label = { Text(item.statusLabel) },
                                leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) })
                            AssistChip(onClick = { action = TitleAction.RATING }, label = { Text(if (item.rating == 0) "Rate" else "${item.rating} / 5") },
                                leadingIcon = { Icon(Icons.Outlined.StarBorder, null, Modifier.size(18.dp)) })
                            AssistChip(onClick = { action = TitleAction.COLLECTION }, label = {
                                Text(item.collection.ifBlank { "Add to collection" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }, leadingIcon = { Icon(Icons.Outlined.CollectionsBookmark, null, Modifier.size(18.dp)) })
                        }
                    }
                }
            }
            if (item.type.supportsSeasons) {
                item(key = "season-heading", contentType = "heading") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Seasons", style = MaterialTheme.typography.titleMedium)
                            item.seasonSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        TextButton(onClick = { seasonNumber = 0 }, enabled = item.seasons.size < 500) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("Add season")
                        }
                    }
                }
                items(seasons, key = { "season:${it.number}" }, contentType = { "season" }) { season ->
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(season.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(season.progress.label + if (season.rating > 0) " · ★ ${season.rating}" else "",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { seasonNumber = season.number }) { Icon(Icons.Outlined.Edit, "Edit ${season.displayName}") }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(enabled = season.watched > 0, onClick = { onSave(item.withSeason(season.withWatched(season.watched - 1))) }) {
                                    Icon(Icons.Outlined.Remove, "One fewer episode watched in season ${season.number}")
                                }
                                Text(if (season.episodes > 0) "${season.watched} / ${season.episodes} ep" else "${season.watched} watched",
                                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                IconButton(enabled = season.watched < (season.episodes.takeIf { it > 0 } ?: 9999),
                                    onClick = { onSave(item.withSeason(season.withWatched(season.watched + 1))) }) {
                                    Icon(Icons.Outlined.Add, "One more episode watched in season ${season.number}")
                                }
                                if (season.progress != Progress.FINISHED) TextButton(onClick = {
                                    onSave(item.withSeason(season.withProgress(Progress.FINISHED)))
                                }) { Text("Finish") }
                            }
                        }
                    }
                }
                if (item.seasons.isNotEmpty()) item(key = "series-ended", contentType = "switch") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Series has ended", Modifier.weight(1f))
                        Switch(checked = item.seriesEnded, onCheckedChange = { onSave(item.copy(seriesEnded = it)) })
                    }
                }
            }
            if (item.notes.isNotBlank()) item(key = "notes", contentType = "notes") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Text(item.notes)
                }
            }
        }
    }
}
