package com.myshelf.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetails(item: ShelfItem, collections: List<String>, onBack: () -> Unit, onEdit: () -> Unit,
    onSave: (ShelfItem) -> Unit, onRemove: () -> Unit) {
    var action by rememberSaveable(item.id) { mutableStateOf<TitleAction?>(null) }
    var seasonNumber by rememberSaveable(item.id) { mutableStateOf<Int?>(null) }
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
            IconButton(onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove title") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.align(Alignment.CenterHorizontally).width(170.dp).aspectRatio(.69f)) {
                Cover(item, Modifier.fillMaxSize())
            }
            Text(item.title, style = MaterialTheme.typography.headlineSmall)
            val metadata = listOf(item.year, item.genre, item.creator).filter { it.isNotBlank() }
            if (metadata.isNotEmpty()) Text(metadata.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { action = TitleAction.STATUS }, label = { Text(item.statusLabel) },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) })
                AssistChip(onClick = { action = TitleAction.RATING }, label = { Text(if (item.rating == 0) "Rate" else "${item.rating} / 5") },
                    leadingIcon = { Icon(Icons.Outlined.StarBorder, null, Modifier.size(18.dp)) })
            }
            TextButton(onClick = { action = TitleAction.COLLECTION }) {
                Icon(Icons.Outlined.CollectionsBookmark, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.collection.ifBlank { "Add to collection" })
            }
            if (item.type.supportsSeasons) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Seasons", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { seasonNumber = 0 }, enabled = item.seasons.size < 500) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("Add season")
                    }
                }
                item.seasonSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                item.seasons.sortedBy { it.number }.forEach { season ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
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
                        Text(if (season.episodes > 0) "${season.watched} / ${season.episodes} episodes" else "${season.watched} episodes watched",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(enabled = season.watched < (season.episodes.takeIf { it > 0 } ?: 9999),
                            onClick = { onSave(item.withSeason(season.withWatched(season.watched + 1))) }) {
                            Icon(Icons.Outlined.Add, "One more episode watched in season ${season.number}")
                        }
                    }
                    if (season.progress != Progress.FINISHED) TextButton(onClick = {
                        onSave(item.withSeason(season.withProgress(Progress.FINISHED)))
                    }) { Text("Mark season finished") }
                }
                if (item.seasons.isNotEmpty()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Series has ended", Modifier.weight(1f))
                    Switch(checked = item.seriesEnded, onCheckedChange = { onSave(item.copy(seriesEnded = it)) })
                }
            }
            if (item.notes.isNotBlank()) {
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                Text(item.notes)
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}
