package com.myshelf.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

@Composable
fun <T> SortMenu(current: T, choices: List<T>, label: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "Sort: ${label(current)}" }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, modifier = Modifier.semantics { selected = option == current },
                    onClick = { onSelect(option); expanded = false }, trailingIcon = {
                        if (option == current) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                    })
            }
        }
    }
}
