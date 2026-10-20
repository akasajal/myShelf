package com.myshelf.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myshelf.app.core.model.CollectionProfile
import com.myshelf.app.core.model.ItemSort
import com.myshelf.app.core.model.MediaType
import com.myshelf.app.core.model.ShelfItem
import com.myshelf.app.core.model.collectionKey
import com.myshelf.app.domain.home.HomeSection
import com.myshelf.app.domain.library.StatusFilter
import com.myshelf.app.domain.library.filterShelf
import com.myshelf.app.domain.library.sortShelf
import com.myshelf.app.features.collections.CollectionEditor
import com.myshelf.app.features.collections.CollectionsScreen
import com.myshelf.app.features.details.TitleAction
import com.myshelf.app.features.details.TitleActionDialog
import com.myshelf.app.features.details.TitleActionsSheet
import com.myshelf.app.features.details.TitleDetails
import com.myshelf.app.features.editor.EditorRoute
import com.myshelf.app.features.home.HomeAddButton
import com.myshelf.app.features.home.HomeScreen
import com.myshelf.app.features.security.AppLockViewModel
import com.myshelf.app.features.settings.SettingsScreen
import com.myshelf.app.features.shelf.ShelfScreen
import com.myshelf.app.features.shelf.ShelfScreenState
import com.myshelf.app.state.ShelfViewModel
import com.myshelf.app.ui.components.AddShelfButton
import com.myshelf.app.ui.components.ShelfFilters
import kotlinx.coroutines.launch

@Composable
fun ShelfApp(vm: ShelfViewModel, appLock: AppLockViewModel, onEnableBiometric: () -> Unit) {
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable(key = "main_tab_with_home") { mutableStateOf(MainTab.HOME) }
    var contexts by rememberSaveable(stateSaver = BrowsingContextsSaver) { mutableStateOf<Map<String, BrowsingContext>>(emptyMap()) }
    val browseKey = when (tab) {
        MainTab.SHELF -> "shelf"
        MainTab.COLLECTIONS -> collection?.let { "collection:${vm.collectionProfile(it).id}" } ?: "collections"
        MainTab.HOME -> "home"
        MainTab.SETTINGS -> "settings"
    }
    val context = contexts[browseKey] ?: BrowsingContext()
    val type = context.media?.let(MediaType::valueOf)
    val query = context.query
    val status = context.status?.let(StatusFilter::valueOf)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickId by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAction by rememberSaveable { mutableStateOf<TitleAction?>(null) }
    var editingCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by vm::editing
    val collectionsListState = rememberLazyListState()
    val gridStates = rememberSaveableStateHolder()
    val detailsState = rememberSaveableStateHolder()
    val homeState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shelfItems = vm.items
    val profiles = vm.collectionProfiles
    val collections = remember(shelfItems, profiles) {
        (profiles.map { it.name } + shelfItems.map { it.collection }).filter { it.isNotBlank() }.distinctBy(::collectionKey)
    }
    val selected = remember(shelfItems, selectedId) { shelfItems.find { it.id == selectedId } }
    val quickItem = remember(shelfItems, quickId) { shelfItems.find { it.id == quickId } }
    creatingCollectionId?.let { id -> key(id) {
        CollectionEditor(CollectionProfile(name = "", id = id), collections, onSave = { profile ->
            val error = vm.addCollection(profile)
            if (error == null) {
                creatingCollectionId = null
                if (tab == MainTab.HOME) { tab = MainTab.COLLECTIONS; collection = profile.name }
            }
            error
        }, onDismiss = { creatingCollectionId = null }, isNew = true)
    } }
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
    BackHandler(enabled = editing == null && selected == null && tab != MainTab.HOME) {
        if (tab == MainTab.COLLECTIONS && collection != null) collection = null
        else { tab = MainTab.HOME; collection = null }
    }
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
                containerColor = MaterialTheme.colorScheme.surface,
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    if (tab != MainTab.SETTINGS && !vm.transferBusy) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            if (tab == MainTab.SHELF || (tab == MainTab.COLLECTIONS && collection != null)) {
                                ShelfFilters(type, onType = { contexts = contexts + (browseKey to context.copy(media = it?.name)) })
                            } else Spacer(Modifier.weight(1f))
                            if (tab == MainTab.HOME) HomeAddButton(
                                onCard = { editing = ShelfItem() },
                                onCollection = { creatingCollectionId = java.util.UUID.randomUUID().toString() })
                            else AddShelfButton(label = if (tab == MainTab.COLLECTIONS && collection == null) "New collection" else "Add to shelf") {
                                if (tab == MainTab.COLLECTIONS && collection == null) creatingCollectionId = java.util.UUID.randomUUID().toString()
                                else editing = ShelfItem(collection = collection.orEmpty())
                            }
                        }
                    }
                },
                bottomBar = {
                    ShelfNavigationBar(selected = tab, enabled = !vm.transferBusy) {
                        tab = it; collection = null
                    }
                }
            ) { padding ->
                if (tab == MainTab.SETTINGS) {
                    SettingsScreen(vm, appLock, onEnableBiometric, Modifier.padding(padding))
                } else if (tab == MainTab.HOME) homeState.SaveableStateProvider("home") {
                    HomeScreen(shelfItems, vm.loadError, onOpen = { selectedId = it.id },
                        onLongPress = { quickId = it.id; quickAction = null },
                        onViewAll = { section ->
                            contexts = contexts + ("shelf" to BrowsingContext(status = if (section == HomeSection.CONTINUE) StatusFilter.ACTIVE.name else null))
                            vm.updateShelfSort(ItemSort.NEWEST)
                            gridStates.removeState("shelf")
                            collection = null; tab = MainTab.SHELF
                        }, modifier = Modifier.padding(padding))
                } else if (tab == MainTab.COLLECTIONS && collection == null) {
                    CollectionsScreen(shelfItems, profiles, vm.collectionsSort, collectionsListState, vm.loadError,
                        canEdit = !vm.transferBusy, onSort = vm::updateCollectionsSort,
                        onOpen = { collection = it }, onEdit = { editingCollection = it }, modifier = Modifier.padding(padding))
                } else gridStates.SaveableStateProvider(browseKey) {
                    ShelfScreen(
                        state = ShelfScreenState(visible, shelfItems.isNotEmpty(), query, status, itemOrder, vm.cardsPerRow, vm.cardDetails, vm.loadError),
                        gridState = rememberLazyGridState(),
                        collection = collection?.takeIf { tab == MainTab.COLLECTIONS }?.let(vm::collectionProfile),
                        canEditCollection = !vm.transferBusy,
                        onEditCollection = { editingCollection = collection },
                        onQuery = { contexts = contexts + (browseKey to context.copy(query = it)) },
                        onStatus = { contexts = contexts + (browseKey to context.copy(status = it?.name)) },
                        onSort = { order ->
                            val name = collection
                            if (name == null) vm.updateShelfSort(order) else vm.updateCollectionSort(name, order)
                        }, onOpen = { selectedId = it.id }, onLongPress = { quickId = it.id; quickAction = null },
                        modifier = Modifier.padding(padding))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp)
            .padding(bottom = if (editing == null && selected == null) 96.dp else 24.dp).navigationBarsPadding())
    }
}
