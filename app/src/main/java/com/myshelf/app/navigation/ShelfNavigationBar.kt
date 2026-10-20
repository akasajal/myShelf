package com.myshelf.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
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
        val motion = updateTransition(selected.ordinal, label = "Navigation pill")
        // The leading edge moves first; the trailing edge catches up like a droplet.
        // Both springs retain their current position and velocity when taps interrupt a move.
        val left = motion.animateFloat(transitionSpec = {
            spring(dampingRatio = .85f, stiffness = if (targetState > initialState) 220f else 500f)
        }, label = "Pill left edge") { it.toFloat() }
        val right = motion.animateFloat(transitionSpec = {
            spring(dampingRatio = .85f, stiffness = if (targetState > initialState) 500f else 220f)
        }, label = "Pill right edge") { it.toFloat() }
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val gap = 4.dp.toPx()
                val width = (size.width - gap * (MainTab.entries.size - 1)) / MainTab.entries.size
                val stride = width + gap
                val start = (left.value * stride).coerceIn(0f, size.width - width)
                val end = (right.value * stride + width).coerceIn(start + width, size.width)
                val squeeze = (abs(right.value - left.value) * 4.dp.toPx()).coerceAtMost(6.dp.toPx())
                drawRoundRect(
                    color = pillColor,
                    topLeft = Offset(start, squeeze), size = Size(end - start, size.height - squeeze * 2),
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
