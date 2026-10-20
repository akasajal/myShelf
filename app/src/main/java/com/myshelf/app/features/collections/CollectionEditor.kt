package com.myshelf.app.features.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myshelf.app.core.model.CollectionIcon
import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.collectionKey
import com.myshelf.app.ui.components.vector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEditor(initial: CollectionProfile, names: List<String>, onSave: (CollectionProfile) -> String?, onDismiss: () -> Unit,
    isNew: Boolean = false) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var description by rememberSaveable(initial.id) { mutableStateOf(initial.description) }
    var icon by rememberSaveable(initial.id) { mutableStateOf(initial.icon) }
    var error by rememberSaveable(initial.id) { mutableStateOf<String?>(null) }
    var discard by rememberSaveable(initial.id) { mutableStateOf(false) }
    val current = initial.copy(name = name.trim(), description = description.trim(), icon = icon)
    val duplicate = names.any { collectionKey(it) != collectionKey(initial.name) && collectionKey(it) == collectionKey(name) }
    val leave = { if (current != initial) discard = true else onDismiss() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard changes?") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    ModalBottomSheet(onDismissRequest = leave, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon.vector, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(if (isNew) "New collection" else "Collection", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(enabled = current.name.isNotBlank() && current.name.length <= 80 && !duplicate, onClick = { error = onSave(current) }) { Text("Save") }
            }
            OutlinedTextField(name, { if (it.length <= 80) { name = it; error = null } }, Modifier.fillMaxWidth(),
                label = { Text("Name") }, singleLine = true, isError = duplicate,
                supportingText = { if (duplicate) Text("This collection name is already in use.") })
            OutlinedTextField(description, { if (it.length <= 1000) description = it }, Modifier.fillMaxWidth(),
                label = { Text("Description") }, minLines = 2, maxLines = 4)
            Row(Modifier.fillMaxWidth()) {
                Text("Icon", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(icon.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CollectionIcon.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { choice ->
                            val selected = choice == icon
                            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f), RoundedCornerShape(14.dp))
                                .selectable(selected, role = Role.RadioButton, onClick = { icon = choice })
                                .semantics { contentDescription = "${choice.label} collection icon" }, contentAlignment = Alignment.Center) {
                                Icon(choice.vector, null, Modifier.size(24.dp), tint = if (selected)
                                    MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
