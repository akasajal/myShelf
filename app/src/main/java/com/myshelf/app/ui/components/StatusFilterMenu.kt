package com.myshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myshelf.app.domain.library.StatusFilter

@Composable
fun StatusFilterMenu(current: StatusFilter?, onSelect: (StatusFilter?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Filter by status: ${current?.label ?: "All statuses"}" },
            colors = ButtonDefaults.textButtonColors(containerColor = if (current == null)
                MaterialTheme.colorScheme.surface.copy(alpha = 0f) else MaterialTheme.colorScheme.primaryContainer)) {
            Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(current?.label ?: "Status")
        }
        DropdownMenu(expanded, { expanded = false }) {
            (listOf<StatusFilter?>(null) + StatusFilter.entries).forEach { choice ->
                val checked = choice == current
                DropdownMenuItem(text = { Text(choice?.label ?: "All statuses") },
                    modifier = Modifier.semantics { selected = checked }.then(if (checked)
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier),
                    onClick = { onSelect(choice); expanded = false },
                    trailingIcon = { if (checked) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) })
            }
        }
    }
}
