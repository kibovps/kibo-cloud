package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ArchivaDatabase
import com.example.data.local.CollectionWithCount
import com.example.data.local.CollectionWithFiles
import com.example.data.local.FileRecordEntity
import com.example.data.local.LinkedFolderEntity
import com.example.data.model.BreadcrumbSegment
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.PendingTransfer
import com.example.data.model.SortOption
import com.example.data.model.StorageLocation
import com.example.data.model.ThemePreference
import com.example.data.model.ViewMode
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.FileExplorerRepository
import com.example.data.repository.VaultStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExplorerUiState(
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val rawItems: List<FileItem> = emptyList(),
    val visibleItems: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: FileCategory? = null,
    val selectedIds: Set<String> = emptySet(),
    val pendingTransfer: PendingTransfer? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ArchivaDatabase.getInstance(application)
    val repository = FileExplorerRepository(application, database.archivaDao())
    private val prefs = UserPreferencesRepository(application)

    val viewMode: StateFlow<ViewMode> = prefs.viewMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ViewMode.LIST,
    )

    val sortOption: StateFlow<SortOption> = prefs.sortOption.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SortOption.NAME_ASC,
    )

    val themePreference: StateFlow<ThemePreference> = prefs.themePreference.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemePreference.SYSTEM,
    )

    val linkedFolders: StateFlow<List<LinkedFolderEntity>> = repository.linkedFolders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val collectionsWithCount: StateFlow<List<CollectionWithCount>> = repository.collectionsWithCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val favorites: StateFlow<List<FileRecordEntity>> = repository.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val recentFiles: StateFlow<List<FileRecordEntity>> = repository.recentFiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val storageLocations: StateFlow<List<StorageLocation>> = linkedFolders.map { linked ->
        buildList {
            add(
                StorageLocation(
                    name = "Bóveda Local",
                    subtitle = "Almacenamiento interno organizado",
                    uriString = repository.vaultRootUri,
                    source = FileSource.VAULT,
                )
            )
            repository.externalAppStorageUri?.let { extUri ->
                add(
                    StorageLocation(
                        name = "Almacenamiento App",
                        subtitle = "Archivos locales de la app",
                        uriString = extUri,
                        source = FileSource.VAULT,
                    )
                )
            }
            linked.forEach { folder ->
                add(
                    StorageLocation(
                        name = folder.displayName,
                        subtitle = "Carpeta del dispositivo (SAF)",
                        uriString = folder.uriString,
                        source = FileSource.SAF,
                    )
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf(
            StorageLocation(
                name = "Bóveda Local",
                subtitle = "Almacenamiento interno organizado",
                uriString = repository.vaultRootUri,
                source = FileSource.VAULT,
            )
        ),
    )

    private val _vaultStats = MutableStateFlow(VaultStats())
    val vaultStats: StateFlow<VaultStats> = _vaultStats.asStateFlow()

    private var pendingTransferSourceSegment: BreadcrumbSegment? = null

    private val _explorerBase = MutableStateFlow(
        ExplorerUiState(
            breadcrumbs = listOf(
                BreadcrumbSegment(
                    name = "Bóveda Local",
                    uriString = repository.vaultRootUri,
                    source = FileSource.VAULT,
                )
            )
        )
    )

    val explorerState: StateFlow<ExplorerUiState> = combine(
        _explorerBase,
        sortOption,
        repository.allFileRecords,
        repository.allCrossRefs,
    ) { base, sort, records, crossRefs ->
        val recordMap = records.associateBy { it.fileUri }
        val refMap = crossRefs.groupBy({ it.fileUri }, { it.collectionId })

        val enriched = base.rawItems.map { item ->
            item.copy(
                isFavorite = recordMap[item.uriString]?.isFavorite ?: false,
                collectionIds = refMap[item.uriString].orEmpty(),
            )
        }

        val query = base.searchQuery.trim().lowercase()
        val filtered = enriched.filter { item ->
            val matchesCategory = base.selectedCategory == null || item.category == base.selectedCategory
            val matchesQuery = query.isEmpty() || item.name.lowercase().contains(query)
            matchesCategory && matchesQuery
        }

        val sorted = sortItems(filtered, sort)
        base.copy(rawItems = enriched, visibleItems = sorted)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _explorerBase.value,
    )

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        refreshVaultStats()
        loadCurrentDirectory()
    }

    fun observeCollectionFiles(collectionId: Long): Flow<CollectionWithFiles?> =
        repository.observeCollectionWithFiles(collectionId)

    fun refreshVaultStats() {
        viewModelScope.launch {
            _vaultStats.value = repository.computeVaultStats()
        }
    }

    fun openRootLocation(
        name: String,
        uriString: String,
        source: FileSource,
        categoryFilter: FileCategory? = null,
    ) {
        _explorerBase.update {
            it.copy(
                breadcrumbs = listOf(
                    BreadcrumbSegment(
                        name = name,
                        uriString = uriString,
                        source = source,
                        rootUriString = uriString,
                        pathSegments = emptyList(),
                    )
                ),
                selectedCategory = categoryFilter,
                searchQuery = "",
                selectedIds = emptySet(),
            )
        }
        loadCurrentDirectory()
    }

    fun openDirectory(item: FileItem) {
        if (!item.isDirectory) return
        _explorerBase.update { state ->
            val current = state.breadcrumbs.lastOrNull()
            val nextSegment = BreadcrumbSegment(
                name = item.name,
                uriString = item.uriString,
                source = item.source,
                rootUriString = current?.rootUriString ?: item.uriString,
                pathSegments = (current?.pathSegments ?: emptyList()) + item.name,
            )
            state.copy(
                breadcrumbs = state.breadcrumbs + nextSegment,
                searchQuery = "",
                selectedCategory = null,
                selectedIds = emptySet(),
            )
        }
        loadCurrentDirectory()
    }

    fun navigateToBreadcrumb(index: Int) {
        val current = _explorerBase.value.breadcrumbs
        if (index !in current.indices) return
        _explorerBase.update {
            it.copy(
                breadcrumbs = current.take(index + 1),
                selectedIds = emptySet(),
                searchQuery = "",
            )
        }
        loadCurrentDirectory()
    }

    fun navigateUp(): Boolean {
        val current = _explorerBase.value.breadcrumbs
        return if (current.size > 1) {
            navigateToBreadcrumb(current.lastIndex - 1)
            true
        } else {
            false
        }
    }

    fun loadCurrentDirectory() {
        val segment = _explorerBase.value.breadcrumbs.lastOrNull() ?: return
        viewModelScope.launch {
            _explorerBase.update { it.copy(isLoading = true, errorMessage = null) }
            repository.listDirectory(segment)
                .onSuccess { items ->
                    _explorerBase.update { it.copy(rawItems = items, isLoading = false) }
                }
                .onFailure { error ->
                    _explorerBase.update {
                        it.copy(
                            rawItems = emptyList(),
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Error al leer la carpeta",
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _explorerBase.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: FileCategory?) {
        _explorerBase.update { it.copy(selectedCategory = category) }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val next = if (viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            prefs.setViewMode(next)
        }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { prefs.setSortOption(option) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefs.setThemePreference(theme) }
    }

    fun toggleItemSelection(item: FileItem) {
        _explorerBase.update { state ->
            val updated = if (item.id in state.selectedIds) {
                state.selectedIds - item.id
            } else {
                state.selectedIds + item.id
            }
            state.copy(selectedIds = updated)
        }
    }

    fun selectAllVisible() {
        val ids = explorerState.value.visibleItems.map { it.id }.toSet()
        _explorerBase.update { it.copy(selectedIds = ids) }
    }

    fun clearSelection() {
        _explorerBase.update { it.copy(selectedIds = emptySet()) }
    }

    fun createFolder(name: String) {
        val current = _explorerBase.value.breadcrumbs.lastOrNull() ?: return
        viewModelScope.launch {
            repository.createFolder(current, name)
                .onSuccess {
                    loadCurrentDirectory()
                    refreshVaultStats()
                    emitMessage("Carpeta \"$name\" creada")
                }
                .onFailure { emitMessage(it.localizedMessage ?: "No se pudo crear la carpeta") }
        }
    }

    fun createTextDocument(fileName: String, content: String, onCreated: (FileItem) -> Unit = {}) {
        val current = _explorerBase.value.breadcrumbs.lastOrNull() ?: return
        viewModelScope.launch {
            repository.createTextDocument(current, fileName, content)
                .onSuccess { created ->
                    loadCurrentDirectory()
                    refreshVaultStats()
                    emitMessage("Documento \"${created.name}\" creado")
                    onCreated(created)
                }
                .onFailure { emitMessage(it.localizedMessage ?: "No se pudo crear el documento") }
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        val parentSegment = _explorerBase.value.breadcrumbs.lastOrNull()
        viewModelScope.launch {
            repository.renameItem(item, newName, parentSegment)
                .onSuccess {
                    clearSelection()
                    loadCurrentDirectory()
                    refreshVaultStats()
                    emitMessage("Renombrado a \"$newName\"")
                }
                .onFailure { emitMessage(it.localizedMessage ?: "No se pudo renombrar") }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        if (items.isEmpty()) return
        val parentSegment = _explorerBase.value.breadcrumbs.lastOrNull()
        viewModelScope.launch {
            repository.deleteItems(items, parentSegment)
                .onSuccess { count ->
                    clearSelection()
                    loadCurrentDirectory()
                    refreshVaultStats()
                    emitMessage("$count elemento(s) eliminado(s)")
                }
                .onFailure { emitMessage(it.localizedMessage ?: "Error al eliminar") }
        }
    }

    fun importUrisToCurrentFolder(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = _explorerBase.value.breadcrumbs.lastOrNull() ?: return
        viewModelScope.launch {
            _explorerBase.update { it.copy(isLoading = true) }
            repository.importUris(uris, current)
                .onSuccess { count ->
                    loadCurrentDirectory()
                    refreshVaultStats()
                    emitMessage("$count archivo(s) importado(s)")
                }
                .onFailure { error ->
                    _explorerBase.update { it.copy(isLoading = false) }
                    emitMessage(error.localizedMessage ?: "Error al importar archivos")
                }
        }
    }

    fun linkSafFolder(treeUri: Uri, openImmediately: Boolean = true) {
        viewModelScope.launch {
            repository.linkSafFolder(treeUri)
                .onSuccess { linked ->
                    emitMessage("Carpeta \"${linked.displayName}\" vinculada")
                    if (openImmediately) {
                        openRootLocation(linked.displayName, linked.uriString, FileSource.SAF)
                    }
                }
                .onFailure { emitMessage(it.localizedMessage ?: "No se pudo vincular la carpeta") }
        }
    }

    fun unlinkSafFolder(uriString: String) {
        viewModelScope.launch {
            repository.unlinkSafFolder(uriString)
            emitMessage("Carpeta desvinculada")
        }
    }

    fun startTransfer(items: List<FileItem>, isMove: Boolean) {
        if (items.isEmpty()) return
        pendingTransferSourceSegment = _explorerBase.value.breadcrumbs.lastOrNull()
        _explorerBase.update {
            it.copy(
                pendingTransfer = PendingTransfer(items, isMove),
                selectedIds = emptySet(),
            )
        }
        val actionWord = if (isMove) "mover" else "copiar"
        emitMessageAsync("Navega a la carpeta destino y pulsa Pegar para $actionWord")
    }

    fun cancelTransfer() {
        pendingTransferSourceSegment = null
        _explorerBase.update { it.copy(pendingTransfer = null) }
    }

    fun executePendingTransferHere() {
        val transfer = _explorerBase.value.pendingTransfer ?: return
        val dest = _explorerBase.value.breadcrumbs.lastOrNull() ?: return
        val sourceSeg = pendingTransferSourceSegment
        viewModelScope.launch {
            _explorerBase.update { it.copy(isLoading = true) }
            repository.transferItems(transfer.items, dest, transfer.isMove, sourceSeg)
                .onSuccess { count ->
                    pendingTransferSourceSegment = null
                    _explorerBase.update { it.copy(pendingTransfer = null) }
                    loadCurrentDirectory()
                    refreshVaultStats()
                    val verb = if (transfer.isMove) "movido(s)" else "copiado(s)"
                    emitMessage("$count elemento(s) $verb")
                }
                .onFailure { error ->
                    _explorerBase.update { it.copy(isLoading = false) }
                    emitMessage(error.localizedMessage ?: "No se pudo completar la operación")
                }
        }
    }

    fun toggleFavorite(item: FileItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun toggleFavoriteByUri(record: FileRecordEntity) {
        viewModelScope.launch {
            val item = record.toFileItem()
            repository.toggleFavorite(item)
        }
    }

    fun markFileOpened(item: FileItem) {
        viewModelScope.launch {
            repository.markFileOpened(item)
        }
    }

    fun savePlaybackPosition(fileUri: String, positionMs: Long) {
        viewModelScope.launch {
            repository.savePlaybackPosition(fileUri, positionMs)
        }
    }

    fun createCollection(name: String, colorIndex: Int) {
        viewModelScope.launch {
            repository.createCollection(name, colorIndex)
                .onSuccess { emitMessage("Colección \"$name\" creada") }
                .onFailure { emitMessage(it.localizedMessage ?: "No se pudo crear") }
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            emitMessage("Colección eliminada")
        }
    }

    fun setFileInCollection(item: FileItem, collectionId: Long, inCollection: Boolean) {
        viewModelScope.launch {
            repository.setFileInCollection(item, collectionId, inCollection)
        }
    }

    private fun emitMessageAsync(msg: String) {
        viewModelScope.launch { _snackbarMessages.emit(msg) }
    }

    private suspend fun emitMessage(msg: String) {
        _snackbarMessages.emit(msg)
    }

    private fun sortItems(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            SortOption.NAME_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            SortOption.DATE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortOption.SIZE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.sizeBytes }
            SortOption.TYPE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.category.ordinal }.thenBy { it.name.lowercase() }
        }
        return items.sortedWith(comparator)
    }
}

fun FileRecordEntity.toFileItem(): FileItem {
    val category = runCatching { FileCategory.valueOf(categoryName) }.getOrDefault(FileCategory.OTHER)
    val source = runCatching { FileSource.valueOf(sourceName) }.getOrDefault(FileSource.VAULT)
    return FileItem(
        id = fileUri,
        uriString = fileUri,
        name = fileName,
        isDirectory = false,
        sizeBytes = sizeBytes,
        lastModified = lastOpenedAt,
        mimeType = mimeType,
        extension = fileName.substringAfterLast('.', ""),
        category = category,
        source = source,
        parentUriString = parentUri,
        isFavorite = isFavorite,
    )
}
