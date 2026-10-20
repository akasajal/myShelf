package com.myshelf.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.myshelf.app.core.model.collectionKey
import com.myshelf.app.data.backup.ImportedShelf
import com.myshelf.app.domain.backup.ImportMode
import com.myshelf.app.domain.backup.previewChanges
import com.myshelf.app.domain.seasons.statusLabel
import com.myshelf.app.state.ShelfViewModel

@Composable
fun ImportPreviewDialog(vm: ShelfViewModel, backup: ImportedShelf) {
    var mode by rememberSaveable { mutableStateOf(ImportMode.SKIP_EXISTING) }
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    val changes = remember(vm.items, backup) { previewChanges(vm.items, backup.items) }
    val existing = remember(vm.items) { vm.items.associateBy { it.id } }
    if (confirmReplace) AlertDialog(onDismissRequest = { if (!vm.transferBusy) confirmReplace = false },
        title = { Text("Replace your entire shelf?") },
        text = { Column {
            Text("Your ${vm.items.size} saved titles will be replaced with ${backup.items.size} titles from this backup.")
            vm.transferMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { vm.importShelf(ImportMode.REPLACE) }, enabled = !vm.transferBusy) {
            Text("Replace shelf", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = { confirmReplace = false }, enabled = !vm.transferBusy) { Text("Back") } })
    else AlertDialog(onDismissRequest = vm::dismissImport,
        title = { Text("Import preview") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportMode.entries.forEach { option ->
                    Row(Modifier.fillMaxWidth().selectable(selected = mode == option, enabled = !vm.transferBusy,
                        role = Role.RadioButton, onClick = { mode = option }).heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == option, onClick = null)
                        Text(option.label, Modifier.padding(start = 8.dp))
                    }
                }
                val updated = if (mode == ImportMode.SKIP_EXISTING) 0 else changes.matching.size
                Text("${changes.added.size} new · $updated updated" + when (mode) {
                    ImportMode.SKIP_EXISTING -> " · ${changes.matching.size} skipped"
                    ImportMode.UPDATE_MATCHING -> " · ${changes.removed.size} kept"
                    ImportMode.REPLACE -> " · ${changes.removed.size} removed"
                }, style = MaterialTheme.typography.bodyMedium)
                Text(when (mode) {
                    ImportMode.SKIP_EXISTING -> "Matching IDs keep their saved details."
                    ImportMode.UPDATE_MATCHING -> "Matching IDs receive all backup details, including covers, status, ratings, seasons, and notes. Other saved titles stay."
                    ImportMode.REPLACE -> "Only titles in this backup will remain."
                }, style = MaterialTheme.typography.bodySmall)
                if (backup.collections.isNotEmpty()) {
                    Text("${backup.collections.size} collection customizations", style = MaterialTheme.typography.bodyMedium)
                    backup.collections.take(20).forEach { profile ->
                        val matched = vm.collectionProfiles.any { it.id == profile.id || collectionKey(it.name) == collectionKey(profile.name) }
                        val action = if (!matched) "Add" else if (mode == ImportMode.SKIP_EXISTING) "Keep" else "Update"
                        Text("$action · ${profile.name} · ${profile.icon.label}", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (mode == ImportMode.REPLACE && vm.collectionProfiles.isNotEmpty()) {
                    Text("Existing collection customizations will be removed.", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                backup.items.take(40).forEach { incoming ->
                    val previous = existing[incoming.id]
                    val action = if (previous == null) "New" else if (mode == ImportMode.SKIP_EXISTING) "Skip" else "Update"
                    Text("$action · ${incoming.title}", style = MaterialTheme.typography.bodyMedium)
                    if (action == "Update") Text(
                        "${previous!!.statusLabel} → ${incoming.statusLabel} · ${previous.seasons.size} → ${incoming.seasons.size} seasons",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (backup.items.size > 40) Text("And ${backup.items.size - 40} more titles.")
                if (mode == ImportMode.REPLACE) changes.removed.take(20).forEach {
                    Text("Remove · ${it.title}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (backup.items.isEmpty() && backup.collections.isEmpty()) Text("This backup is empty.")
                vm.transferMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !vm.transferBusy, onClick = {
            if (mode == ImportMode.REPLACE) confirmReplace = true else vm.importShelf(mode)
        }) { Text("Import") } },
        dismissButton = { TextButton(vm::dismissImport, enabled = !vm.transferBusy) { Text("Cancel") } })
}
