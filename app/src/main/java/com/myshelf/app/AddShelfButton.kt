package com.myshelf.app

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddShelfButton(label: String = "Add to shelf", onAdd: () -> Unit) {
    FloatingActionButton(onClick = onAdd, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Icon(Icons.Outlined.Add, label, Modifier.size(28.dp))
    }
}
