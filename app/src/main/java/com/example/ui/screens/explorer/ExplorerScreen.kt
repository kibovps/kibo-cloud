package com.example.ui.screens.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.SortOption
import com.example.data.model.ViewMode
import com.example.ui.components.BaseDocumentFileExplorer
import com.example.ui.components.CollectionAssignmentSheet
import com.example.ui.components.CreateCollectionDialog
import com.example.ui.components.CreateDocumentDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileActionsSheet
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.components.rememberMediaImageLoader
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: MainViewModel,
    onOpenFile: (FileItem) -> Unit,
    onPickMedia: () -> Unit,
    onPickDocuments: () -> Unit,
    onLinkDeviceFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.explorerState.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val collections by viewModel.collectionsWithCount.collectAsStateWithLifecycle()
    val storageLocations by viewModel.storageLocations.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val imageLoader = rememberMediaImageLoader()

    BackHandler(enabled = state.breadcrumbs.size > 1 || state.selectedIds.isNotEmpty()) {
        if (state.selectedIds.isNotEmpty()) {
            viewModel.clearSelection()
        } else {
            viewModel.navigateUp()
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showRootSwitcher by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var actionTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var detailsTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var collectionTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var pendingDeleteItems by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    val selectionMode = state.selectedIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} seleccionado(s)") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar selección")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllVisible() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todo")
                        }
                        val selectedItems = state.visibleItems.filter { it.id in state.selectedIds }
                        IconButton(onClick = { viewModel.startTransfer(selectedItems, isMove = true) }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Mover")
                        }
                        IconButton(onClick = { viewModel.startTransfer(selectedItems, isMove = false) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar")
                        }
                        IconButton(onClick = { pendingDeleteItems = selectedItems }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showRootSwitcher = true }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("storage_root_switcher"),
                            ) {
                                Text(
                                    text = state.breadcrumbs.lastOrNull()?.name ?: "Explorador",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Cambiar almacenamiento",
                                )
                            }
                            DropdownMenu(
                                expanded = showRootSwitcher,
                                onDismissRequest = { showRootSwitcher = false },
                            ) {
                                storageLocations.forEach { location ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(location.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    location.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                                        onClick = {
                                            showRootSwitcher = false
                                            viewModel.openRootLocation(
                                                name = location.name,
                                                uriString = location.uriString,
                                                source = location.source,
                                            )
                                        },
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Vincular carpeta del dispositivo...") },
                                    leadingIcon = { Icon(Icons.Filled.AddLink, contentDescription = null) },
                                    onClick = {
                                        showRootSwitcher = false
                                        onLinkDeviceFolder()
                                    },
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (state.breadcrumbs.size > 1) {
                            IconButton(
                                onClick = { viewModel.navigateUp() },
                                modifier = Modifier.testTag("explorer_up_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Subir carpeta")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("explorer_view_mode_toggle"),
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Cambiar vista",
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("explorer_sort_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(visible = fabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FabActionPill(
                            label = "Vincular carpeta",
                            icon = Icons.Filled.AddLink,
                            onClick = {
                                fabExpanded = false
                                onLinkDeviceFolder()
                            },
                        )
                        FabActionPill(
                            label = "Importar archivos",
                            icon = Icons.Filled.FileUpload,
                            onClick = {
                                fabExpanded = false
                                onPickDocuments()
                            },
                        )
                        FabActionPill(
                            label = "Importar multimedia",
                            icon = Icons.Filled.PermMedia,
                            onClick = {
                                fabExpanded = false
                                onPickMedia()
                            },
                        )
                        FabActionPill(
                            label = "Nuevo documento",
                            icon = Icons.AutoMirrored.Filled.NoteAdd,
                            onClick = {
                                fabExpanded = false
                                showNewDocDialog = true
                            },
                        )
                        FabActionPill(
                            label = "Nueva carpeta",
                            icon = Icons.Filled.CreateNewFolder,
                            onClick = {
                                fabExpanded = false
                                showNewFolderDialog = true
                            },
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    modifier = Modifier.testTag("explorer_fab"),
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Agregar",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.pendingTransfer?.let { transfer ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (transfer.isMove) "Mover ${transfer.items.size} elemento(s)" else "Copiar ${transfer.items.size} elemento(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Abre la carpeta destino y pulsa Pegar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        TextButton(onClick = { viewModel.cancelTransfer() }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { viewModel.executePendingTransferHere() },
                            modifier = Modifier.testTag("paste_here_button"),
                        ) {
                            Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pegar")
                        }
                    }
                }
            }

            // Persistent real-time search bar at the top of the explorer
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por nombre de archivo...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("explorer_search_clear"),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("explorer_search_input"),
            )

            BaseDocumentFileExplorer(
                breadcrumbs = state.breadcrumbs,
                items = state.visibleItems,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                viewMode = viewMode,
                selectedIds = state.selectedIds,
                imageLoader = imageLoader,
                onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) },
                onRefresh = { viewModel.loadCurrentDirectory() },
                onItemClick = { item ->
                    if (selectionMode) {
                        viewModel.toggleItemSelection(item)
                    } else if (item.isDirectory) {
                        viewModel.openDirectory(item)
                    } else {
                        onOpenFile(item)
                    }
                },
                onItemLongClick = { item -> viewModel.toggleItemSelection(item) },
                onItemMoreClick = { item -> actionTargetItem = item },
                headerSlot = {
                    val filterOptions = listOf(
                        null to "Todos",
                        FileCategory.VIDEO to "Videos",
                        FileCategory.AUDIO to "Audio",
                        FileCategory.IMAGE to "Imágenes",
                        FileCategory.PDF to "PDF",
                        FileCategory.TEXT to "Notas",
                        FileCategory.DOCUMENT to "Docs",
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filterOptions) { (cat, label) ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = { viewModel.setCategoryFilter(cat) },
                                label = { Text(label) },
                            )
                        }
                    }
                },
                emptyContent = {
                    ExplorerEmptyState(
                        hasFilters = state.selectedCategory != null || state.searchQuery.isNotEmpty(),
                        onClearFilters = {
                            viewModel.setCategoryFilter(null)
                            viewModel.setSearchQuery("")
                        },
                        onImportMedia = onPickMedia,
                        onImportDocs = onPickDocuments,
                        onNewFolder = { showNewFolderDialog = true },
                    )
                },
            )
        }
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = "Nueva carpeta",
            label = "Nombre de la carpeta",
            confirmLabel = "Crear",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showNewFolderDialog = false
            },
        )
    }

    if (showNewDocDialog) {
        CreateDocumentDialog(
            onDismiss = { showNewDocDialog = false },
            onConfirm = { name, content ->
                showNewDocDialog = false
                viewModel.createTextDocument(name, content) { created ->
                    onOpenFile(created)
                }
            },
        )
    }

    actionTargetItem?.let { item ->
        FileActionsSheet(
            item = item,
            onDismiss = { actionTargetItem = null },
            onOpen = { onOpenFile(item) },
            onOpenExternal = { Formatters.openWithExternalApp(context, item.uriString, item.mimeType) },
            onToggleFavorite = { viewModel.toggleFavorite(item) },
            onManageCollections = { collectionTargetItem = item },
            onRename = { renameTargetItem = item },
            onMove = { viewModel.startTransfer(listOf(item), isMove = true) },
            onCopy = { viewModel.startTransfer(listOf(item), isMove = false) },
            onShare = { Formatters.shareFile(context, item.uriString, item.mimeType, item.name) },
            onDetails = { detailsTargetItem = item },
            onDelete = { pendingDeleteItems = listOf(item) },
        )
    }

    renameTargetItem?.let { item ->
        TextInputDialog(
            title = "Renombrar",
            label = "Nuevo nombre",
            initialValue = item.name,
            confirmLabel = "Guardar",
            onDismiss = { renameTargetItem = null },
            onConfirm = { newName ->
                viewModel.renameItem(item, newName)
                renameTargetItem = null
            },
        )
    }

    detailsTargetItem?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { detailsTargetItem = null },
        )
    }

    collectionTargetItem?.let { item ->
        CollectionAssignmentSheet(
            item = item,
            collections = collections,
            onDismiss = { collectionTargetItem = null },
            onToggleCollection = { colId, assign ->
                viewModel.setFileInCollection(item, colId, assign)
            },
            onCreateNewCollection = { showCreateCollectionDialog = true },
        )
    }

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, colorIdx ->
                viewModel.createCollection(name, colorIdx)
                showCreateCollectionDialog = false
            },
        )
    }

    if (pendingDeleteItems.isNotEmpty()) {
        DeleteConfirmDialog(
            count = pendingDeleteItems.size,
            itemName = pendingDeleteItems.singleOrNull()?.name,
            onDismiss = { pendingDeleteItems = emptyList() },
            onConfirm = {
                viewModel.deleteItems(pendingDeleteItems)
                pendingDeleteItems = emptyList()
            },
        )
    }
}

@Composable
private fun FabActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun ExplorerEmptyState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onImportMedia: () -> Unit,
    onImportDocs: () -> Unit,
    onNewFolder: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_vault),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(22.dp)),
                )
                Text(
                    text = if (hasFilters) "Sin resultados" else "Esta carpeta está vacía",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (hasFilters) {
                        "No hay archivos que coincidan con tu búsqueda o filtro."
                    } else {
                        "Importa videos, fotos, música o documentos, o crea una nueva subcarpeta."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (hasFilters) {
                    Button(onClick = onClearFilters) {
                        Text("Quitar filtros")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onImportMedia) {
                            Text("Multimedia")
                        }
                        OutlinedButton(onClick = onImportDocs) {
                            Text("Documentos")
                        }
                    }
                    TextButton(onClick = onNewFolder) {
                        Text("Crear subcarpeta")
                    }
                }
            }
        }
    }
}
