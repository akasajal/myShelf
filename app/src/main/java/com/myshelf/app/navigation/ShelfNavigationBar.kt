package com.myshelf.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.myshelf.app.R
import kotlin.math.abs

@Composable
internal fun ShelfNavigationBar(selected: MainTab, enabled: Boolean, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val pillColor = MaterialTheme.colorScheme.primaryContainer
        val position = remember { Animatable(selected.ordinal.toFloat(), visibilityThreshold = .0005f) }
        LaunchedEffect(selected) {
            position.animateTo(selected.ordinal.toFloat(), spring(dampingRatio = 1f, stiffness = 320f))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val gap = 4.dp.toPx()
                val width = (size.width - gap * (MainTab.entries.size - 1)) / MainTab.entries.size
                val stride = width + gap
                val start = (position.value * stride).coerceIn(0f, size.width - width)
                // One position controls the whole pill. Stretch follows its speed and
                // fades naturally to zero, without independent edges snapping into place.
                val stretch = minOf(abs(position.velocity) * stride * .018f, width * .16f,
                    start, (size.width - start - width).coerceAtLeast(0f))
                val squeeze = if (width > 0f) stretch / width * 8.dp.toPx() else 0f
                drawRoundRect(
                    color = pillColor,
                    topLeft = Offset(start - stretch, squeeze),
                    size = Size(width + stretch * 2, size.height - squeeze * 2),
                    cornerRadius = CornerRadius(28.dp.toPx())
                )
            }
            Row(Modifier.fillMaxWidth().selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MainTab.entries.forEach { tab ->
                    val active = selected == tab
                    val foreground by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "Navigation content color")
                    Surface(
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
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
}
