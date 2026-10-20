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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun ShelfFilters(type: MediaType?, onType: (MediaType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val offset = with(LocalDensity.current) { 72.dp.roundToPx() }
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * .6f).dp
    val menuWidth = minOf(200.dp, (configuration.screenWidthDp - 32).coerceAtLeast(0).dp)
    Box {
        FloatingActionButton(onClick = { expanded = true }, modifier = Modifier.size(56.dp).semantics {
            contentDescription = "Filter: ${type?.label ?: "All media"}"
        }, shape = RoundedCornerShape(18.dp), containerColor = if (type == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (type == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Outlined.FilterList, null, Modifier.size(26.dp))
        }
        if (expanded) Popup(alignment = Alignment.BottomStart, offset = IntOffset(0, -offset),
            onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.heightIn(max = maxHeight).width(menuWidth).verticalScroll(rememberScrollState())) {
                FilterOption("All media", Icons.Outlined.Apps, type == null) { onType(null); expanded = false }
                FilterDivider()
                MediaType.entries.forEachIndexed { index, media ->
                    val icon = when (media) {
                        MediaType.MOVIE -> Icons.Outlined.Movie
                        MediaType.SERIES -> Icons.Outlined.VideoLibrary
                        MediaType.TV -> Icons.Outlined.LiveTv
                        MediaType.COMIC -> Icons.Outlined.AutoStories
                        MediaType.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
                        MediaType.OTHER -> Icons.Outlined.MoreHoriz
                    }
                    FilterOption(media.label, icon, type == media) { onType(media); expanded = false }
                    if (index < MediaType.entries.lastIndex) FilterDivider()
                }
            }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { selected = active },
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(20.dp))
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (active) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FilterDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
}
