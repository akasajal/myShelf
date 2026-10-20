package com.myshelf.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class TitleAction(val label: String) { STATUS("Change status"), RATING("Rate"), COLLECTION("Move collection"), REMOVE("Remove") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleActionsSheet(item: ShelfItem, onAction: (TitleAction) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(item.title, Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge)
        TitleAction.entries.forEach { action ->
            ListItem(headlineContent = { Text(action.label) }, leadingContent = {
                Icon(when (action) {
                    TitleAction.STATUS -> Icons.Outlined.CheckCircle
                    TitleAction.RATING -> Icons.Outlined.StarBorder
                    TitleAction.COLLECTION -> Icons.Outlined.CollectionsBookmark
                    TitleAction.REMOVE -> Icons.Outlined.DeleteOutline
                }, null)
            }, modifier = Modifier.clickable { onAction(action) })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TitleActionDialog(item: ShelfItem, action: TitleAction, collections: List<String>, onSave: (ShelfItem) -> Unit, onDismiss: () -> Unit) {
    when (action) {
        TitleAction.RATING -> RatingDialog(item.rating, onDismiss) { onSave(item.copy(rating = it)); onDismiss() }
        TitleAction.COLLECTION -> CollectionDialog(item, collections, onDismiss) { onSave(item.copy(collection = it)); onDismiss() }
        TitleAction.STATUS -> StatusDialog(item, onDismiss) { onSave(it); onDismiss() }
        TitleAction.REMOVE -> Unit
    }
}

@Composable
private fun RatingDialog(initial: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("My rating") }, text = {
        Column {
            Text(if (rating == 0) "Unrated" else "$rating / 5")
            Row(Modifier.fillMaxWidth()) {
                (1..5).forEach { star -> IconButton(onClick = { rating = if (rating == star) 0 else star }, modifier = Modifier.weight(1f)) {
                    Icon(if (star <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, "Rate $star out of 5",
                        tint = MaterialTheme.colorScheme.primary)
                } }
            }
        }
    }, confirmButton = { TextButton({ onSave(rating) }) { Text("Done") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun CollectionDialog(item: ShelfItem, collections: List<String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var collection by rememberSaveable { mutableStateOf(item.collection) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move collection") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(collection, { collection = it }, label = { Text("Collection") }, singleLine = true)
            TextButton(onClick = { collection = "" }) { Text("No collection") }
            collections.forEach { name -> TextButton(onClick = { collection = name }) { Text(name) } }
        }
    }, confirmButton = { TextButton({ onSave(collection.trim()) }) { Text("Move") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun StatusDialog(item: ShelfItem, onDismiss: () -> Unit, onSave: (ShelfItem) -> Unit) {
    val tracked = item.type.supportsSeasons && item.seasons.isNotEmpty()
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (tracked) "Season status" else "Status") }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            if (tracked) item.seasons.forEach { season ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(season.displayName, Modifier.weight(1f))
                    Box {
                        TextButton({ expanded = season.number }) { Text(season.progress.label) }
                        DropdownMenu(expanded == season.number, { expanded = null }) {
                            Progress.entries.forEach { value -> DropdownMenuItem(text = { Text(value.label) }, onClick = {
                                onSave(item.withSeason(season.withProgress(value)))
                            }) }
                        }
                    }
                }
            } else Progress.entries.forEach { value ->
                TextButton(onClick = { onSave(item.copy(progress = value)) }, modifier = Modifier.fillMaxWidth()) { Text(value.label) }
            }
        }
    }, confirmButton = { TextButton(onDismiss) { Text("Close") } })
}
