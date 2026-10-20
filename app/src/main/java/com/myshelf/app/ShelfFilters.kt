package com.myshelf.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun ShelfFilters(type: MediaType?, genre: String?, genres: List<String>, onType: (MediaType?) -> Unit, onGenre: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val offset = with(LocalDensity.current) { 72.dp.roundToPx() }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .6f).dp
    Box {
        FloatingActionButton(onClick = { expanded = true }, modifier = Modifier.size(56.dp).semantics {
            contentDescription = "Filter: ${type?.label ?: "All media"}${genre?.let { ", $it" }.orEmpty()}"
        }, shape = RoundedCornerShape(18.dp), containerColor = if (type == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (type == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Outlined.FilterList, null, Modifier.size(26.dp))
        }
        if (expanded) Popup(alignment = Alignment.BottomStart, offset = IntOffset(0, -offset),
            onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            Column(Modifier.heightIn(max = maxHeight).widthIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterOption("All media", Icons.Outlined.Apps, type == null) { onType(null); expanded = false }
                MediaType.entries.forEach { media ->
                    val icon = when (media) {
                        MediaType.MOVIE -> Icons.Outlined.Movie
                        MediaType.SERIES -> Icons.Outlined.VideoLibrary
                        MediaType.TV -> Icons.Outlined.LiveTv
                        MediaType.COMIC -> Icons.Outlined.AutoStories
                        MediaType.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
                        MediaType.OTHER -> Icons.Outlined.MoreHoriz
                    }
                    FilterOption(media.label, icon, type == media) { onType(media); expanded = false }
                }
                if (type != null && genres.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                        Text("Genre · ${type.label}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(12.dp))
                    }
                    FilterOption("All genres", Icons.Outlined.Category, genre == null) { onGenre(null); expanded = false }
                    genres.forEach { value ->
                        FilterOption(value, Icons.Outlined.Category, genre == value) { onGenre(value); expanded = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.semantics { selected = active },
        shape = RoundedCornerShape(16.dp), shadowElevation = 3.dp,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(22.dp))
            Text(label, Modifier.weight(1f, fill = false))
            if (active) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
        }
    }
}
