package com.myshelf.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray

internal val SeasonListSaver = Saver<List<Season>, String>(
    save = { ShelfJson.encodeSeasons(it).toString() },
    restore = { ShelfJson.decodeSeasons(JSONArray(it)) }
)

@Composable
fun SeasonSection(seasons: List<Season>, ended: Boolean, onChange: (List<Season>) -> Unit, onEnded: (Boolean) -> Unit) {
    // Zero denotes a new season; existing seasons are addressed by their stable number.
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Seasons", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { editing = 0 }, enabled = seasons.size < 500) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Text("Add season")
        }
    }
    seasons.sortedBy { it.number }.forEach { season ->
        Row(Modifier.fillMaxWidth().clickable { editing = season.number }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(season.displayName)
                val episodes = if (season.episodes > 0) "${season.watched}/${season.episodes} episodes" else "${season.watched} episodes watched"
                Text("${season.progress.label} · $episodes", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (season.rating > 0) Text("★ ${season.rating}", color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Outlined.ChevronRight, "Edit season ${season.number}")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
    }
    if (seasons.isNotEmpty()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Series has ended", Modifier.weight(1f))
        Switch(checked = ended, onCheckedChange = onEnded)
    }
    editing?.let { selected ->
        val existing = seasons.find { it.number == selected }
        val initial = existing ?: Season((1..999).first { n -> seasons.none { it.number == n } })
        SeasonDialog(initial, existing != null, seasons.filter { it.number != selected }.map { it.number }.toSet(),
            onDismiss = { editing = null },
            onSave = { updated ->
                onChange((seasons.filterNot { it.number == selected } + updated).sortedBy { it.number })
                editing = null
            },
            onRemove = { onChange(seasons.filterNot { it.number == selected }); editing = null })
    }
}

@Composable
internal fun SeasonDialog(initial: Season, existing: Boolean, occupied: Set<Int>, onDismiss: () -> Unit,
    onSave: (Season) -> Unit, onRemove: () -> Unit) {
    var number by rememberSaveable(initial.number) { mutableStateOf(initial.number.toString()) }
    var name by rememberSaveable(initial.number) { mutableStateOf(initial.name) }
    var total by rememberSaveable(initial.number) { mutableStateOf(initial.episodes.takeIf { it > 0 }?.toString().orEmpty()) }
    var watched by rememberSaveable(initial.number) { mutableStateOf(initial.watched.toString()) }
    var progress by rememberSaveable(initial.number) { mutableStateOf(initial.progress) }
    var rating by rememberSaveable(initial.number) { mutableIntStateOf(initial.rating) }
    var statusMenu by remember { mutableStateOf(false) }
    val seasonNumber = number.toIntOrNull()
    val episodeCount = if (total.isBlank()) 0 else total.toIntOrNull()
    val watchedCount = watched.toIntOrNull()
    val duplicate = seasonNumber in occupied
    val valid = seasonNumber != null && seasonNumber in 1..999 && !duplicate &&
        episodeCount != null && episodeCount in 0..9999 && watchedCount != null && watchedCount in 0..9999 &&
        (episodeCount == 0 || watchedCount <= episodeCount)
    fun updateEpisodeProgress() {
        val count = watched.toIntOrNull() ?: return
        val max = total.toIntOrNull() ?: 0
        progress = when {
            max > 0 && count == max -> Progress.FINISHED
            count > 0 -> if (progress == Progress.PAUSED) Progress.PAUSED else Progress.ACTIVE
            else -> Progress.PLANNED
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing) "Edit season" else "Add season") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(number, { if (it.length <= 3 && it.all(Char::isDigit)) number = it },
                    label = { Text("Season number") }, singleLine = true, isError = duplicate,
                    supportingText = { if (duplicate) Text("This season already exists.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(name, { if (it.length <= 200) name = it }, label = { Text("Season name (optional)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(watched, {
                        if (it.length <= 4 && it.all(Char::isDigit)) { watched = it; updateEpisodeProgress() }
                    }, Modifier.weight(1f), label = { Text("Watched") }, singleLine = true,
                        isError = watchedCount != null && episodeCount != null && episodeCount > 0 && watchedCount > episodeCount,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(total, {
                        if (it.length <= 4 && it.all(Char::isDigit)) { total = it; updateEpisodeProgress() }
                    }, Modifier.weight(1f), label = { Text("Episodes") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Text("Leave episodes blank if unknown.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { statusMenu = true }) { Text(progress.label) }
                        DropdownMenu(statusMenu, { statusMenu = false }) {
                            Progress.entries.forEach { value ->
                                DropdownMenuItem(text = { Text(value.label) }, onClick = {
                                    progress = value
                                    if (value == Progress.FINISHED && (total.toIntOrNull() ?: 0) > 0) watched = total
                                    statusMenu = false
                                })
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = if (rating == star) 0 else star }, modifier = Modifier.weight(1f)) {
                            Icon(if (star <= rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                "Season rating $star out of 5", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (existing) TextButton(onClick = onRemove) { Text("Remove season", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            val count = requireNotNull(episodeCount)
            onSave(Season(requireNotNull(seasonNumber), progress, rating,
                if (progress == Progress.FINISHED && count > 0) count else requireNotNull(watchedCount), count, name.trim()))
        }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
