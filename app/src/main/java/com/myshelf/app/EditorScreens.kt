package com.myshelf.app

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditor(initial: ShelfItem, collections: List<String>, onBack: () -> Unit,
    onSave: (ShelfItem) -> Unit, onDelete: (String) -> Unit) {
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var type by rememberSaveable(initial.id) { mutableStateOf(initial.type) }
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
    var imageError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val current = initial.copy(title = title.trim(), type = type, genre = genre.trim(), collection = collection.trim(),
        cover = cover.trim(), creator = creator.trim(), year = year.trim(), rating = rating, progress = progress, notes = notes.trim())
    val leave = { if (current != initial) confirmDiscard = true else onBack() }
    BackHandler(onBack = leave)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                cover = uri.toString(); imageError = false
            } catch (_: SecurityException) { imageError = true }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Remove this title?") },
        text = { Text("This removes the title and your notes from your shelf.") },
        confirmButton = { TextButton({ onDelete(initial.id) }) { Text("Remove") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard changes?") },
        confirmButton = { TextButton(onBack) { Text("Discard") } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep editing") } })
    Scaffold(topBar = {
        TopAppBar(title = { Text("Make it yours", fontFamily = FontFamily.Serif) }, navigationIcon = {
            IconButton(leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = { TextButton({ onSave(current) }, enabled = title.isNotBlank()) { Text("Save") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card { Cover(current, Modifier.size(width = 100.dp, height = 145.dp)) }
                Column(Modifier.padding(start = 20.dp)) {
                    Text("Your edition. Your cover.", fontFamily = FontFamily.Serif, fontSize = 20.sp)
                    TextButton({ picker.launch(arrayOf("image/*")) }) { Text("Choose cover image") }
                    if (cover.isNotBlank()) TextButton({ cover = "" }) { Text("Remove cover") }
                }
            }
            if (imageError) Text("Couldn't keep access to that image. Please choose another.", color = MaterialTheme.colorScheme.error)
            EditorField("Title *", title, { title = it })
            Text("Media type", fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MediaType.entries) { media -> FilterChip(type == media, { if (type != media) { type = media; genre = "" } }, label = { Text(media.singular) }) }
            }
            EditorField("Genre · ${type.label} only", genre, { genre = it })
            EditorField("Collection (e.g. Marvel, DC)", collection, { collection = it })
            if (collections.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(collections) { name -> SuggestionChip({ collection = name }, label = { Text(name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorField("Year", year, { year = it }, Modifier.weight(1f))
                EditorField("Creator / author", creator, { creator = it }, Modifier.weight(2f))
            }
            Text("Where you're at", fontFamily = FontFamily.Serif, fontSize = 21.sp)
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
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("A thought to remember") }, minLines = 3)
            Button({ onSave(current) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save to my shelf") }
            TextButton({ confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Remove from shelf", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}
