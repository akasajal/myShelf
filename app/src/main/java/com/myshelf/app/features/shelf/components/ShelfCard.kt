package com.myshelf.app.features.shelf.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myshelf.app.core.model.CardDetails
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.domain.seasons.seasonSummary
import com.myshelf.app.domain.seasons.statusLabel
import com.myshelf.app.ui.components.Cover

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(item: ShelfItem, compact: Boolean, details: CardDetails, onClick: () -> Unit, onLongClick: () -> Unit) {
    val minimal = details == CardDetails.COMPACT
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Title actions")) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Box {
                Cover(item, Modifier.fillMaxWidth().aspectRatio(.69f))
                Surface(Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(5.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                    Text(item.type.singular.uppercase(), Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        fontSize = 8.sp, letterSpacing = .5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(item.title, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.SemiBold,
            minLines = if (minimal) 1 else 2, maxLines = if (minimal) 1 else 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        if (!minimal) Text(listOf(item.genre, item.year).filter { it.isNotBlank() }.joinToString(" · "), fontSize = if (compact) 10.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.statusLabel, fontSize = if (compact) 9.sp else 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.rating > 0) Text("★ ${item.rating}", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 10.sp else 12.sp)
        }
        item.seasonSummary?.let {
            Text(it, fontSize = if (compact) 9.sp else 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
