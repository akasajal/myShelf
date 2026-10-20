package com.myshelf.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import org.json.JSONArray

@Composable
fun EditorRoute(initial: ShelfItem, items: List<ShelfItem>, onBack: () -> Unit, onSave: (ShelfItem) -> Unit,
    onDelete: (String) -> Unit, onOpenExisting: (ShelfItem) -> Unit) {
    var pending by rememberSaveable(initial.id) { mutableStateOf<String?>(null) }
    val candidate = pending?.let { ShelfJson.decode(JSONArray(it)).single() }
    val duplicate = candidate?.let { findDuplicate(items, it) }
    if (candidate != null && duplicate != null) AlertDialog(onDismissRequest = { pending = null },
        title = { Text("Already on your shelf") },
        text = { Text("${duplicate.title} (${duplicate.type.singular}) already exists. Opening it discards this draft.") },
        confirmButton = {
            androidx.compose.foundation.layout.Column {
                TextButton(onClick = { pending = null; onOpenExisting(duplicate) }) { Text("Open existing") }
                TextButton(onClick = { pending = null; onSave(candidate) }) { Text("Save anyway") }
            }
        },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Keep editing") } })
    ItemEditor(initial, items.map { it.collection }.filter { it.isNotBlank() }.distinct(), onBack,
        onSave = { updated ->
            val checkDuplicate = items.none { it.id == updated.id } || initial.title != updated.title || initial.type != updated.type
            if (checkDuplicate && findDuplicate(items, updated) != null) pending = ShelfJson.encode(listOf(updated)).toString()
            else onSave(updated)
        }, onDelete, initialMediaTypeSelected = items.any { it.id == initial.id })
}
