package com.myshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import com.myshelf.app.core.model.ShelfItem

@Composable
fun Cover(item: ShelfItem, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop,
    highQuality: Boolean = false, backdrop: Boolean = false,
    placeholder: @Composable () -> Unit = { CoverFallback(item) }) {
    val context = LocalContext.current
    val request = remember(context, item.cover, highQuality, backdrop) {
        val variant = if (backdrop) "backdrop" else if (highQuality) "detail" else "thumbnail"
        ImageRequest.Builder(context).data(item.cover.ifBlank { null })
            .memoryCacheKey("${item.cover}:$variant")
            .precision(if (highQuality) Precision.EXACT else Precision.INEXACT)
            .apply { if (backdrop) size(128) }.build()
    }
    var loaded by remember(request) { mutableStateOf(false) }
    Box(modifier) {
        if (!loaded) placeholder()
        AsyncImage(model = request, contentDescription = "Cover for ${item.title}",
            contentScale = contentScale, filterQuality = if (highQuality) FilterQuality.High else FilterQuality.Low,
            modifier = Modifier.matchParentSize(), onState = { loaded = it is AsyncImagePainter.State.Success })
    }
}

@Composable
private fun CoverFallback(item: ShelfItem) {
    if (item.title.isBlank()) {
        CoverDoodle()
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF607564), Color(0xFF263F36)))),
        contentAlignment = Alignment.Center) {
        val small = maxWidth < 100.dp
        val fontSize = when { small -> 14.sp; maxWidth < 140.dp -> 18.sp; else -> 24.sp }
        Text(item.title, Modifier.padding(if (small) 8.dp else 16.dp), fontFamily = FontFamily.Serif,
            color = Color(0xFFF4EEDC), fontSize = fontSize, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 5, overflow = TextOverflow.Ellipsis)
    }
}
