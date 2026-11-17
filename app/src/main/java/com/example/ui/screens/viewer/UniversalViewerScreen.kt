package com.example.ui.screens.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.ui.components.CollectionAssignmentSheet
import com.example.ui.components.CreateCollectionDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalViewerScreen(
    uriString: String,
    title: String,
    mimeType: String,
    categoryName: String,
    sizeBytes: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val category = remember(categoryName) {
        runCatching { FileCategory.valueOf(categoryName) }.getOrDefault(FileCategory.OTHER)
    }

    val collections by viewModel.collectionsWithCount.collectAsStateWithLifecycle()
    val allRecords by viewModel.repository.allFileRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val allRefs by viewModel.repository.allCrossRefs.collectAsStateWithLifecycle(initialValue = emptyList())

    val currentRecord = remember(allRecords, uriString) { allRecords.find { it.fileUri == uriString } }
    val isFavorite = currentRecord?.isFavorite ?: false
    val assignedCollections = remember(allRefs, uriString) {
        allRefs.filter { it.fileUri == uriString }.map { it.collectionId }
    }

    val fileItem = remember(uriString, title, mimeType, category, sizeBytes, isFavorite, assignedCollections) {
        FileItem(
            id = uriString,
            uriString = uriString,
            name = title,
            isDirectory = false,
            sizeBytes = sizeBytes,
            lastModified = System.currentTimeMillis(),
            mimeType = mimeType,
            extension = title.substringAfterLast('.', ""),
            category = category,
            source = if (uriString.startsWith("file:")) FileSource.VAULT else FileSource.SAF,
            parentUriString = "",
            isFavorite = isFavorite,
            collectionIds = assignedCollections,
        )
    }

    var savedPos by remember { mutableLongStateOf(0L) }
    var posLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        viewModel.markFileOpened(fileItem)
        savedPos = viewModel.repository.getSavedPlaybackPosition(uriString)
        posLoaded = true
    }

    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    val isDarkViewer = category == FileCategory.VIDEO || category == FileCategory.IMAGE

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (isDarkViewer) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(fileItem) },
                        modifier = Modifier.testTag("viewer_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color(0xFFEF4444) else if (isDarkViewer) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showCollectionSheet = true }) {
                        Icon(Icons.Filled.CollectionsBookmark, contentDescription = "Colecciones")
                    }
                    IconButton(onClick = { Formatters.shareFile(context, uriString, mimeType, title) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir")
                    }
                    IconButton(onClick = { Formatters.openWithExternalApp(context, uriString, mimeType) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir con otra app")
                    }
                },
                colors = if (isDarkViewer) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (category) {
                FileCategory.VIDEO -> {
                    if (posLoaded) {
                        VideoPlayerView(
                            uriString = uriString,
                            initialPositionMs = savedPos,
                            onSavePosition = { pos -> viewModel.savePlaybackPosition(uriString, pos) },
                            onOpenExternal = { Formatters.openWithExternalApp(context, uriString, mimeType) },
                        )
                    }
                }
                FileCategory.AUDIO -> {
                    if (posLoaded) {
                        AudioPlayerView(
                            uriString = uriString,
                            title = title,
                            initialPositionMs = savedPos,
                            onSavePosition = { pos -> viewModel.savePlaybackPosition(uriString, pos) },
                            onOpenExternal = { Formatters.openWithExternalApp(context, uriString, mimeType) },
                        )
                    }
                }
                FileCategory.IMAGE -> {
                    ImageViewerView(uriString = uriString, title = title)
                }
                FileCategory.PDF -> {
                    PdfViewerView(
                        uriString = uriString,
                        onOpenExternal = { Formatters.openWithExternalApp(context, uriString, mimeType) },
                    )
                }
                FileCategory.TEXT -> {
                    TextDocumentViewerView(
                        uriString = uriString,
                        repository = viewModel.repository,
                        onSavedMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                    )
                }
                else -> {
                    ExternalDocumentFallbackView(
                        title = title,
                        mimeType = mimeType,
                        category = category,
                        sizeBytes = sizeBytes,
                        onOpenExternal = { Formatters.openWithExternalApp(context, uriString, mimeType) },
                        onShare = { Formatters.shareFile(context, uriString, mimeType, title) },
                    )
                }
            }
        }
    }

    if (showCollectionSheet) {
        CollectionAssignmentSheet(
            item = fileItem,
            collections = collections,
            onDismiss = { showCollectionSheet = false },
            onToggleCollection = { colId, assign ->
                viewModel.setFileInCollection(fileItem, colId, assign)
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
}
