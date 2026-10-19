package com.myshelf.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditor(initial: ShelfItem, collections: List<String>, onBack: () -> Unit,
    onSave: (ShelfItem) -> Unit, onDelete: (String) -> Unit, initialMediaTypeSelected: Boolean = true) {
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var type by rememberSaveable(initial.id) { mutableStateOf(initial.type) }
    var mediaTypeSelected by rememberSaveable(initial.id) { mutableStateOf(initialMediaTypeSelected) }
    var mediaMenuExpanded by remember { mutableStateOf(false) }
    var genre by rememberSaveable(initial.id) { mutableStateOf(initial.genre) }
    var collection by rememberSaveable(initial.id) { mutableStateOf(initial.collection) }
    var cover by rememberSaveable(initial.id) { mutableStateOf(initial.cover) }
    var creator by rememberSaveable(initial.id) { mutableStateOf(initial.creator) }
    var year by rememberSaveable(initial.id) { mutableStateOf(initial.year) }
    var rating by rememberSaveable(initial.id) { mutableIntStateOf(initial.rating) }
    var progress by rememberSaveable(initial.id) { mutableStateOf(initial.progress) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showCoverSheet by rememberSaveable(initial.id) { mutableStateOf(false) }
    val coverImporter: CoverPickerViewModel = viewModel()
    val coverBusy = coverImporter.busyFor == initial.id
    val current = initial.copy(title = title.trim(), type = type, genre = genre.trim(), collection = collection.trim(),
        cover = cover.trim(), creator = creator.trim(), year = year.trim(), rating = rating, progress = progress, notes = notes.trim())
    val exit = { coverImporter.cancel(initial.id); onBack() }
    val leave = { if (current != initial) confirmDiscard = true else exit() }
    BackHandler(onBack = leave)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coverImporter.choose(initial.id, uri)
        }
    }
    LaunchedEffect(coverImporter.result) {
        coverImporter.result?.takeIf { it.itemId == initial.id }?.let {
            cover = it.uri
            showCoverSheet = false
            coverImporter.consume()
        }
    }
    if (showCoverSheet) CoverSheet(initial.id, cover.isNotBlank(), coverImporter,
        onChoose = { showCoverSheet = false; picker.launch(arrayOf("image/*")) },
        onRemove = { cover = ""; coverImporter.cancel(initial.id); showCoverSheet = false },
        onDismiss = { showCoverSheet = false })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Remove this title?") },
        text = { Text("Your rating and notes will also be deleted.") },
        confirmButton = { TextButton({ coverImporter.cancel(initial.id); onDelete(initial.id) }) { Text("Remove") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard changes?") },
        confirmButton = { TextButton(exit) { Text("Discard") } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep editing") } })
    Scaffold(topBar = {
        TopAppBar(title = { Text("Title details", fontFamily = FontFamily.Serif) }, navigationIcon = {
            IconButton(leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = { TextButton({ onSave(current) }, enabled = title.isNotBlank() && mediaTypeSelected && !coverBusy) { Text("Save") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                Card(onClick = { coverImporter.clearError(); showCoverSheet = true },
                    modifier = Modifier.size(width = 140.dp, height = 200.dp)
                        .semantics { contentDescription = if (cover.isBlank()) "Choose cover" else "Change cover" }) {
                    Cover(current, Modifier.fillMaxSize(), placeholder = { CoverDoodle() })
                }
                if (coverBusy) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp))
                }
            }
            if (!showCoverSheet && coverImporter.errorFor == initial.id) coverImporter.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            EditorField("Title *", title, { title = it })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Media type", Modifier.weight(1f))
                Box {
                    TextButton(onClick = { mediaMenuExpanded = true },
                        modifier = Modifier.semantics { contentDescription = "Media type: ${if (mediaTypeSelected) type.singular else "Select"}" }) {
                        Text(if (mediaTypeSelected) type.singular else "Select")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = mediaMenuExpanded, onDismissRequest = { mediaMenuExpanded = false }) {
                        MediaType.entries.forEach { media ->
                            DropdownMenuItem(text = { Text(media.singular) }, onClick = {
                                if (type != media) { type = media; genre = "" }
                                mediaTypeSelected = true
                                mediaMenuExpanded = false
                            }, trailingIcon = {
                                if (mediaTypeSelected && type == media) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                            })
                        }
                    }
                }
            }
            EditorField(if (mediaTypeSelected) "Genre · ${type.label}" else "Genre", genre, { genre = it })
            EditorField("Collection", collection, { collection = it })
            if (collections.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(collections) { name -> SuggestionChip({ collection = name }, label = { Text(name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorField("Year", year, { year = it }, Modifier.weight(1f))
                EditorField("Creator / author", creator, { creator = it }, Modifier.weight(2f))
            }
            Text("Status", fontFamily = FontFamily.Serif, fontSize = 21.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Progress.entries.forEach { state -> FilterChip(progress == state, { progress = state }, label = { Text(state.label) }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My rating", Modifier.weight(1f))
                Text(if (rating == 0) "Unrated" else "$rating / 5", fontSize = 12.sp)
            }
            Row {
                (1..5).forEach { star -> IconButton({ rating = if (rating == star) 0 else star }) {
                    Icon(if (star <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder, "Rate $star out of 5",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                } }
            }
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 3)
            TextButton({ confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Remove from shelf", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}
