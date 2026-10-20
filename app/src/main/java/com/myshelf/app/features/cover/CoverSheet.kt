package com.myshelf.app.features.cover

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myshelf.app.ui.components.Cover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSheet(id: String, hasCover: Boolean, importer: CoverPickerViewModel,
    onChoose: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val busy = importer.busyFor == id
    var enteringLink by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(bottom = 24.dp)) {
            Text("Cover", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (enteringLink) {
                OutlinedTextField(value = link, onValueChange = { link = it; importer.clearError() },
                    enabled = !busy, label = { Text("Image link") }, supportingText = { Text("Use a direct HTTPS image link.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { enteringLink = false; importer.clearError() }, enabled = !busy) { Text("Back") }
                    Button(onClick = { importer.fromLink(id, link) }, enabled = !busy && link.isNotBlank()) { Text("Add cover") }
                }
            } else {
                CoverOption("Choose image", Icons.Outlined.PhotoLibrary, !busy, onChoose)
                CoverOption("Paste image", Icons.Outlined.ContentPaste, !busy) {
                    try {
                        val clip = clipboard.primaryClip
                        val uri = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).uri else null
                        if (uri?.scheme == "content") importer.choose(id, uri)
                        else importer.showError(id, "No image in the clipboard. Try Paste image link.")
                    } catch (_: Exception) { importer.showError(id, "Can't access the clipboard image.") }
                }
                CoverOption("Paste image link", Icons.Outlined.Link, !busy) {
                    // Read the clipboard only after the user requests a paste.
                    link = try {
                        val clip = clipboard.primaryClip
                        val item = if (clip != null && clip.itemCount > 0) clip.getItemAt(0) else null
                        item?.text?.toString() ?: item?.uri?.toString().orEmpty()
                    } catch (_: Exception) { "" }
                    importer.clearError(); enteringLink = true
                }
                if (hasCover) CoverOption("Remove cover", Icons.Outlined.DeleteOutline, !busy, onRemove)
            }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp))
                TextButton(onClick = { importer.cancel(id) }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Cancel") }
            }
            if (importer.errorFor == id) importer.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun CoverOption(title: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick))
}
