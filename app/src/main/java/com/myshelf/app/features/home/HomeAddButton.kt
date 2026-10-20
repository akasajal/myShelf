package com.myshelf.app.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeAddButton(onCard: () -> Unit, onCollection: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val motion = updateTransition(open, label = "Home add menu")
    val rotation by motion.animateFloat(transitionSpec = {
        spring(dampingRatio = .6f, stiffness = 400f)
    }, label = "Home add rotation") { if (it) 45f else 0f }
    val reveal = motion.animateFloat(transitionSpec = {
        spring(dampingRatio = if (targetState) .65f else 1f, stiffness = 350f)
    }, label = "Home add reveal") { if (it) 1f else 0f }
    BackHandler(open) { open = false }
    Row(modifier, horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        // Keep the slot measured when closed, and drive every reveal directly
        // from the open/close transition instead of animating its weighted width.
        Box(Modifier.weight(1f).height(56.dp).clipToBounds(), contentAlignment = Alignment.CenterEnd) {
            if (motion.currentState || motion.targetState) {
                Row(Modifier.fillMaxWidth().padding(end = 10.dp).graphicsLayer {
                    val progress = reveal.value
                    translationX = (1f - progress) * size.width * .35f
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = .92f + .08f * progress
                    scaleY = .92f + .08f * progress
                }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAddAction("Card", Icons.Outlined.Style, Modifier.weight(1f), enabled = open) { open = false; onCard() }
                    HomeAddAction("Collection", Icons.Outlined.CreateNewFolder, Modifier.weight(1f), enabled = open) { open = false; onCollection() }
                }
            }
        }
        FloatingActionButton(onClick = { open = !open },
            modifier = Modifier.size(56.dp).semantics { stateDescription = if (open) "Expanded" else "Collapsed" }, shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Outlined.Add, if (open) "Close add menu" else "Add card or collection",
                Modifier.size(28.dp).rotate(rotation))
        }
    }
}

@Composable
private fun HomeAddAction(label: String, icon: ImageVector, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp), shape = RoundedCornerShape(18.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
