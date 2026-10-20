package com.myshelf.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddShelfButton(onAdd: (MediaType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val offset = with(LocalDensity.current) { 72.dp.roundToPx() }
    val maxMenuHeight = (LocalConfiguration.current.screenHeightDp * .6f).dp
    Box {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 6.dp) {
            Box(Modifier.size(56.dp).combinedClickable(
                role = Role.Button, onClick = { expanded = false; onAdd(null) },
                onLongClickLabel = "Choose media type", onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }).semantics { contentDescription = "Add to shelf" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, null, Modifier.size(28.dp))
            }
        }
        if (expanded) Popup(alignment = Alignment.BottomEnd, offset = IntOffset(0, -offset),
            onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            Column(Modifier.heightIn(max = maxMenuHeight).verticalScroll(rememberScrollState()).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                MediaType.entries.forEach { type ->
                    val icon = when (type) {
                        MediaType.MOVIE -> Icons.Outlined.Movie
                        MediaType.SERIES -> Icons.Outlined.VideoLibrary
                        MediaType.TV -> Icons.Outlined.LiveTv
                        MediaType.COMIC -> Icons.Outlined.AutoStories
                        MediaType.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
                        MediaType.OTHER -> Icons.Outlined.MoreHoriz
                    }
                    Surface(onClick = { expanded = false; onAdd(type) }, shape = RoundedCornerShape(16.dp),
                        shadowElevation = 3.dp, color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(type.label)
                            Icon(icon, null, Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}
