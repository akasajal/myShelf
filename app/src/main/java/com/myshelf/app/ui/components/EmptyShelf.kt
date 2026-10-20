package com.myshelf.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyShelf(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.AutoStories, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, fontFamily = FontFamily.Serif, fontSize = 25.sp, modifier = Modifier.padding(vertical = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
