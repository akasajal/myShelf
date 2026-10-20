package com.myshelf.app.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeAddButton(onCard: () -> Unit, onCollection: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 45f else 0f,
        spring(dampingRatio = .6f, stiffness = 400f), label = "Home add rotation")
    BackHandler(open) { open = false }
    Row(modifier, horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(open, modifier = Modifier.weight(1f),
            enter = expandHorizontally(spring(dampingRatio = .65f, stiffness = 350f), expandFrom = Alignment.End) +
                slideInHorizontally(spring(dampingRatio = .65f, stiffness = 350f)) { it / 2 } + fadeIn(),
            exit = shrinkHorizontally(spring(dampingRatio = 1f, stiffness = 500f), shrinkTowards = Alignment.End) + fadeOut()) {
            Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeAddAction("Card", Icons.Outlined.Style, Modifier.weight(1f)) { open = false; onCard() }
                HomeAddAction("Collection", Icons.Outlined.CreateNewFolder, Modifier.weight(1f)) { open = false; onCollection() }
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
private fun HomeAddAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp), shape = RoundedCornerShape(18.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
