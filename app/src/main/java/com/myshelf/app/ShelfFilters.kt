package com.myshelf.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

@Composable
fun ShelfFilters(type: MediaType?, genre: String?, genres: List<String>, onType: (MediaType?) -> Unit, onGenre: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.semantics {
            contentDescription = "Filter: ${type?.label ?: "All media"}${genre?.let { ", $it" }.orEmpty()}"
        }) {
            Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (type == null) "Filter" else "Filter •")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text("Media type", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            DropdownMenuItem(text = { Text("All media") }, onClick = { onType(null); expanded = false },
                modifier = Modifier.semantics { selected = type == null }, trailingIcon = {
                    if (type == null) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                })
            MediaType.entries.forEach { media ->
                DropdownMenuItem(text = { Text(media.label) }, onClick = { onType(media); expanded = false },
                    modifier = Modifier.semantics { selected = type == media }, trailingIcon = {
                        if (type == media) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                    })
            }
            if (type != null && genres.isNotEmpty()) {
                HorizontalDivider()
                Text("Genre · ${type.label}", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                DropdownMenuItem(text = { Text("All genres") }, onClick = { onGenre(null); expanded = false },
                    modifier = Modifier.semantics { selected = genre == null }, trailingIcon = {
                        if (genre == null) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                    })
                genres.forEach { value ->
                    DropdownMenuItem(text = { Text(value) }, onClick = { onGenre(value); expanded = false },
                        modifier = Modifier.semantics { selected = value == genre }, trailingIcon = {
                            if (genre == value) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                        })
                }
            }
        }
    }
}
