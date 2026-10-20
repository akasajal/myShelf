package com.myshelf.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.myshelf.app.R

@Composable
internal fun ShelfNavigationBar(selected: MainTab, enabled: Boolean, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MainTab.entries.forEach { tab ->
                val active = selected == tab
                val foreground = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = foreground.copy(alpha = if (enabled) 1f else .38f)
                ) {
                    Column(Modifier.selectable(selected = active, enabled = enabled,
                        role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(horizontal = 2.dp, vertical = 10.dp).heightIn(min = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                        when (tab) {
                            MainTab.HOME -> Icon(Icons.Outlined.Home, null)
                            MainTab.SHELF -> Icon(painterResource(R.drawable.ic_shelves), null)
                            MainTab.COLLECTIONS -> Icon(Icons.Outlined.CollectionsBookmark, null)
                            MainTab.SETTINGS -> Icon(Icons.Outlined.Settings, null)
                        }
                        Text(when (tab) {
                            MainTab.HOME -> "Home"
                            MainTab.SHELF -> "My shelf"
                            MainTab.COLLECTIONS -> "Collections"
                            MainTab.SETTINGS -> "Settings"
                        }, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}
