package com.myshelf.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.launch

private val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
private val Green: Color @Composable get() = MaterialTheme.colorScheme.primary
private val Paper: Color @Composable get() = MaterialTheme.colorScheme.surface

class MainActivity : FragmentActivity() {
    private val appLock: AppLockViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { appLock.finishBiometric(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode in listOf(BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                appLock.finishBiometric(false, if (cancelled) null else "Biometric unavailable. Use your PIN.")
            }
        })
        enableEdgeToEdge()
        setContent {
            val vm: ShelfViewModel = viewModel()
            ShelfTheme(vm.themeMode, vm.accent) {
                SideEffect {
                    if (appLock.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                val shelfState = rememberSaveableStateHolder()
                if (appLock.locked) UnlockScreen(appLock, { requestBiometric(BiometricAction.UNLOCK) }, { moveTaskToBack(true) })
                else shelfState.SaveableStateProvider("shelf") {
                    ShelfApp(vm, appLock) { requestBiometric(BiometricAction.ENABLE) }
                }
            }
        }
    }

    private fun requestBiometric(action: BiometricAction) {
        if (!appLock.beginBiometric(action)) return
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (action == BiometricAction.ENABLE) "Enable biometric unlock" else "Unlock My Shelf")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(if (action == BiometricAction.UNLOCK) "Use PIN" else "Cancel").build())
    }
    override fun onStart() {
        super.onStart()
        appLock.onForeground()
    }
    override fun onStop() {
        if (!isChangingConfigurations) {
            appLock.onBackground()
            biometricPrompt.cancelAuthentication()
        }
        super.onStop()
    }
}

@Composable
private fun ShelfApp(vm: ShelfViewModel, appLock: AppLockViewModel, onEnableBiometric: () -> Unit) {
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var contexts by rememberSaveable(stateSaver = BrowsingContextsSaver) { mutableStateOf<Map<String, BrowsingContext>>(emptyMap()) }
    val browseKey = if (tab == 0) "shelf" else collection?.let { "collection:${vm.collectionProfile(it).id}" } ?: "collections"
    val context = contexts[browseKey] ?: BrowsingContext()
    val type = context.media?.let(MediaType::valueOf)
    val query = context.query
    val status = context.status?.let(StatusFilter::valueOf)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAction by rememberSaveable { mutableStateOf<TitleAction?>(null) }
    var editingCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by vm::editing
    val collectionsListState = rememberLazyListState()
    val gridStates = rememberSaveableStateHolder()
    val detailsState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shelfItems = vm.items
    val profiles = vm.collectionProfiles
    val collections = remember(shelfItems, profiles) {
        (profiles.map { it.name } + shelfItems.map { it.collection }).filter { it.isNotBlank() }.distinctBy(::collectionKey)
    }
    val selected = remember(shelfItems, selectedId) { shelfItems.find { it.id == selectedId } }
    val quickItem = remember(shelfItems, quickId) { shelfItems.find { it.id == quickId } }
    editingCollection?.let { original -> key(original) {
        CollectionEditor(vm.collectionProfile(original), collections, onSave = { updated ->
            val error = vm.updateCollection(original, updated)
            if (error == null) {
                if (collection?.let(::collectionKey) == collectionKey(original)) collection = updated.name
                editingCollection = null
            }
            error
        }, onDismiss = { editingCollection = null })
    } }
    val removeTitle: (String) -> Unit = remove@{ id ->
        val removed = vm.delete(id) ?: return@remove
        if (selectedId == id) selectedId = null
        if (editing?.id == id) editing = null
        quickId = null; quickAction = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar("Removed ${removed.item.title}", actionLabel = "Undo", withDismissAction = true,
                    duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) vm.undoDelete(removed)
        }
    }
    BackHandler(enabled = editing == null && selected == null && tab == 1 && collection != null) { collection = null }
    if (quickItem != null) {
        val action = quickAction
        if (action == null) TitleActionsSheet(quickItem, onAction = {
            if (it == TitleAction.REMOVE) removeTitle(quickItem.id) else quickAction = it
        }, onDismiss = { quickId = null })
        else key(quickItem.id, action) {
            TitleActionDialog(quickItem, action, collections, vm::save) { quickId = null; quickAction = null }
        }
    }
    val itemOrder = collection?.let(vm::sortInsideCollection) ?: vm.shelfSort
    val visible = remember(shelfItems, type, collection, query, status, itemOrder) {
        sortShelf(filterShelf(shelfItems, type, null, collection, query, status), itemOrder)
    }
    Box(Modifier.fillMaxSize()) {
    when {
        editing != null -> EditorRoute(editing!!, vm.items, collections,
            onBack = { editing = null }, onSave = { vm.save(it); editing = null },
            onDelete = removeTitle,
            onOpenExisting = { editing = null; selectedId = it.id })
        selected != null -> detailsState.SaveableStateProvider(selected.id) {
            TitleDetails(selected, collections, onBack = { selectedId = null }, onEdit = { editing = selected },
                onSave = vm::save, onRemove = { removeTitle(selected.id) })
        }
        else -> Scaffold(
            containerColor = Paper,
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                if (tab != 2 && !vm.transferBusy) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (tab == 0 || collection != null) {
                            ShelfFilters(type, onType = { contexts = contexts + (browseKey to context.copy(media = it?.name)) })
                        } else Spacer(Modifier.weight(1f))
                        AddShelfButton {
                            editing = ShelfItem(collection = collection.orEmpty())
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Paper) {
                    NavigationBarItem(selected = tab == 0, enabled = !vm.transferBusy, onClick = { tab = 0; collection = null },
                        icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) }, label = { Text("My shelf") })
                    NavigationBarItem(selected = tab == 1, enabled = !vm.transferBusy, onClick = { tab = 1; collection = null },
                        icon = { Icon(Icons.Outlined.CollectionsBookmark, null) }, label = { Text("Collections") })
                    NavigationBarItem(selected = tab == 2, enabled = !vm.transferBusy, onClick = { tab = 2 },
                        icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            if (tab == 2) {
                SettingsScreen(vm, appLock, onEnableBiometric, Modifier.padding(padding))
            } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(20.dp))
                val collectionProfile = collection?.takeIf { tab == 1 }?.let(vm::collectionProfile)
                Row(Modifier.fillMaxWidth().padding(bottom = if (collectionProfile?.description.isNullOrBlank()) 20.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (collectionProfile != null) {
                        Icon(collectionProfile.icon.vector, null, Modifier.size(24.dp), tint = Green)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (tab == 0) "My Shelf" else collectionProfile?.name ?: "Collections", style = MaterialTheme.typography.headlineSmall,
                        color = Ink, modifier = Modifier.weight(1f))
                    if (collectionProfile != null) IconButton(onClick = { editingCollection = collectionProfile.name }, enabled = !vm.transferBusy) {
                        Icon(Icons.Outlined.Edit, "Customize collection")
                    }
                }
                collectionProfile?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, Modifier.padding(bottom = 16.dp), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                vm.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (tab == 1 && collection == null) {
                    val groups = remember(shelfItems, profiles, status, vm.collectionsSort) {
                        sortCollections(filterShelf(shelfItems, null, null, null, "", status), vm.collectionsSort, profiles, includeEmpty = status == null)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${groups.size} collections", Modifier.weight(1f), fontSize = 12.sp, color = Green)
                        StatusFilterMenu(status) { contexts = contexts + (browseKey to context.copy(status = it?.name)) }
                        SortMenu(vm.collectionsSort, CollectionSort.entries, { it.label }, vm::updateCollectionsSort)
                    }
                    if (groups.isEmpty()) EmptyShelf(if (status == null) "No collections yet" else "No matches",
                        if (status == null) "Assign a collection when adding a title." else "Try another status filter.", Modifier.weight(1f))
                    else LazyColumn(state = collectionsListState, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(groups, key = { it.key }, contentType = { "collection" }) { group ->
                            Card(onClick = { collection = group.name }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(group.profile.icon.vector, null, tint = Green)
                                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                        Text(group.name, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                        if (group.profile.description.isNotBlank()) Text(group.profile.description, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${group.items.size} titles · ${group.items.map { it.type }.distinct().size} media types", fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { editingCollection = group.name }, enabled = !vm.transferBusy) {
                                        Icon(Icons.Outlined.Edit, "Customize ${group.name}")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(query, { contexts = contexts + (browseKey to context.copy(query = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("Search shelf") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${visible.size} titles", fontSize = 12.sp, color = Green, modifier = Modifier.weight(1f))
                        StatusFilterMenu(status) { contexts = contexts + (browseKey to context.copy(status = it?.name)) }
                        key(collection) {
                            SortMenu(itemOrder, ItemSort.entries, { it.label }) { order ->
                                val name = collection
                                if (name == null) vm.updateShelfSort(order) else vm.updateCollectionSort(name, order)
                            }
                        }
                    }
                    if (visible.isEmpty()) EmptyShelf(if (vm.items.isEmpty()) "No titles yet" else "No matches",
                        if (vm.items.isEmpty()) "Add a title to get started." else "Try another filter.", Modifier.weight(1f))
                    else gridStates.SaveableStateProvider(browseKey) {
                        val gridState = rememberLazyGridState()
                        LazyVerticalGrid(GridCells.Fixed(vm.cardsPerRow), state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(if (vm.cardsPerRow == 2) 16.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                            items(visible, key = { it.id }, contentType = { "title" }) { item ->
                                ShelfCard(item, compact = vm.cardsPerRow > 2, details = vm.cardDetails,
                                    onClick = { selectedId = item.id }, onLongClick = { quickId = item.id; quickAction = null })
                            }
                        }
                    }
                }
            }
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp)
        .padding(bottom = if (editing == null && selected == null) 96.dp else 24.dp).navigationBarsPadding())
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfCard(item: ShelfItem, compact: Boolean, details: CardDetails, onClick: () -> Unit, onLongClick: () -> Unit) {
    val minimal = details == CardDetails.COMPACT
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Title actions")) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Box {
                Cover(item, Modifier.fillMaxWidth().aspectRatio(.69f))
                Surface(Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(5.dp), color = Paper.copy(alpha = .94f)) {
                    Text(item.type.singular.uppercase(), Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        fontSize = 8.sp, letterSpacing = .5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(item.title, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.SemiBold,
            minLines = if (minimal) 1 else 2, maxLines = if (minimal) 1 else 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        if (!minimal) Text(listOf(item.genre, item.year).filter { it.isNotBlank() }.joinToString(" · "), fontSize = if (compact) 10.sp else 12.sp,
            color = Ink.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.statusLabel, fontSize = if (compact) 9.sp else 10.sp, color = Green, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.rating > 0) Text("★ ${item.rating}", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 10.sp else 12.sp)
        }
        item.seasonSummary?.let {
            Text(it, fontSize = if (compact) 9.sp else 10.sp, color = Ink.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
