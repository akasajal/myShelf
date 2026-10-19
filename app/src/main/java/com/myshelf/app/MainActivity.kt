package com.myshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage

private val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
private val Green: Color @Composable get() = MaterialTheme.colorScheme.primary
private val Paper: Color @Composable get() = MaterialTheme.colorScheme.surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ShelfViewModel = viewModel()
            ShelfTheme(vm.themeMode, vm.accent) {
                ShelfApp(vm)
            }
        }
    }
}

@Composable
private fun ShelfApp(vm: ShelfViewModel = viewModel()) {
    var typeName by rememberSaveable { mutableStateOf<String?>(null) }
    val type = typeName?.let(MediaType::valueOf)
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by vm::editing
    val visible = filterShelf(vm.items, type, genre, collection, query)
    when {
        editing != null -> ItemEditor(editing!!, vm.items.map { it.collection }.filter { it.isNotBlank() }.distinct(),
            onBack = { editing = null }, onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
            initialMediaTypeSelected = vm.items.any { it.id == editing!!.id } || type != null)
        else -> Scaffold(
            containerColor = Paper,
            floatingActionButton = {
                if (tab != 2 && !vm.transferBusy) {
                ExtendedFloatingActionButton(onClick = { editing = ShelfItem(type = type ?: MediaType.MOVIE, collection = collection.orEmpty()) }, containerColor = Green,
                    contentColor = MaterialTheme.colorScheme.onPrimary, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add to shelf") })
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Paper) {
                    NavigationBarItem(selected = tab == 0, enabled = !vm.transferBusy, onClick = { tab = 0; collection = null },
                        icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) }, label = { Text("My shelf") })
                    NavigationBarItem(selected = tab == 1, enabled = !vm.transferBusy, onClick = { tab = 1; typeName = null; genre = null; collection = null },
                        icon = { Icon(Icons.Outlined.CollectionsBookmark, null) }, label = { Text("Collections") })
                    NavigationBarItem(selected = tab == 2, enabled = !vm.transferBusy, onClick = { tab = 2 },
                        icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            if (tab == 2) {
                SettingsScreen(vm, Modifier.padding(padding))
            } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(20.dp))
                Text(if (tab == 0) "My Shelf" else collection ?: "Collections", fontFamily = FontFamily.Serif,
                    fontSize = 38.sp, color = Ink, modifier = Modifier.padding(bottom = 20.dp))
                vm.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (tab == 1 && collection == null) {
                    val groups = vm.items.filter { it.collection.isNotBlank() }.groupBy { it.collection.lowercase() }
                    if (groups.isEmpty()) EmptyShelf("No collections yet", "Assign a collection when adding a title.", Modifier.weight(1f))
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(groups.keys.toList()) { key ->
                            val group = groups.getValue(key)
                            Card(onClick = { collection = group.first().collection }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CollectionsBookmark, null, tint = Green)
                                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                        Text(group.first().collection, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                        Text("${group.size} titles · ${group.map { it.type }.distinct().size} media types", fontSize = 12.sp)
                                    }
                                    Icon(Icons.Outlined.ChevronRight, null)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search shelf") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                        item { FilterChip(type == null, { typeName = null; genre = null }, label = { Text("All") }) }
                        items(MediaType.entries) { media -> FilterChip(type == media, { typeName = media.name; genre = null }, label = { Text(media.label) }) }
                    }
                    if (type != null) {
                        val genres = vm.items.filter { it.type == type }.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
                        if (genres.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(genre == null, { genre = null }, label = { Text("All genres") }) }
                            items(genres) { g -> FilterChip(genre == g, { genre = g }, label = { Text(g) }) }
                        }
                    }
                    Text("${visible.size} titles", fontSize = 12.sp, color = Green, modifier = Modifier.padding(vertical = 12.dp))
                    if (visible.isEmpty()) EmptyShelf(if (vm.items.isEmpty()) "No titles yet" else "No matches",
                        if (vm.items.isEmpty()) "Add a title to get started." else "Try another filter.", Modifier.weight(1f))
                    else LazyVerticalGrid(GridCells.Adaptive(145.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(visible, key = { it.id }) { item -> ShelfCard(item) { editing = item } }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun EmptyShelf(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.AutoStories, null, Modifier.size(48.dp), tint = Green)
        Text(title, fontFamily = FontFamily.Serif, fontSize = 25.sp, modifier = Modifier.padding(vertical = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(message, color = Ink.copy(alpha = .6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Cover(item: ShelfItem, modifier: Modifier = Modifier, placeholder: @Composable () -> Unit = { CoverFallback(item) }) {
    SubcomposeAsyncImage(model = item.cover.ifBlank { null }, contentDescription = "Cover for ${item.title}",
        contentScale = ContentScale.Crop, modifier = modifier,
        loading = { placeholder() }, error = { placeholder() })
}

@Composable
private fun CoverFallback(item: ShelfItem) {
    if (item.title.isBlank()) {
        CoverDoodle()
        return
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF607564), Color(0xFF263F36))))
        .padding(16.dp), contentAlignment = Alignment.Center) {
        Text(item.title, fontFamily = FontFamily.Serif, color = Color(0xFFF4EEDC), fontSize = 24.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ShelfCard(item: ShelfItem, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Box {
                Cover(item, Modifier.fillMaxWidth().aspectRatio(.69f))
                Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(6.dp), color = Paper.copy(alpha = .94f)) {
                    Text(item.type.singular.uppercase(), Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 9.sp, letterSpacing = .8.sp)
                }
            }
        }
        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(listOf(item.genre, item.year).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 12.sp, color = Ink.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.progress.label, fontSize = 10.sp, color = Green, modifier = Modifier.weight(1f))
            if (item.rating > 0) Text("★ ${item.rating}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
    }
}
