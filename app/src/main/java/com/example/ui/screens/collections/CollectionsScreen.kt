package com.example.ui.screens.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CollectionWithCount
import com.example.data.local.FileRecordEntity
import com.example.data.model.FileItem
import com.example.ui.components.CreateCollectionDialog
import com.example.ui.components.FileThumbnailBox
import com.example.ui.components.rememberMediaImageLoader
import com.example.ui.theme.CollectionPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.toFileItem
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    viewModel: MainViewModel,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collections by viewModel.collectionsWithCount.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val imageLoader = rememberMediaImageLoader()

    var selectedTab by remember { mutableIntStateOf(0) }
    var openedCollectionId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = openedCollectionId != null) {
        openedCollectionId = null
    }

    if (openedCollectionId != null) {
        val collectionDetail by viewModel.observeCollectionFiles(openedCollectionId!!)
            .collectAsStateWithLifecycle(initialValue = null)

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(collectionDetail?.collection?.name ?: "Colección") },
                    navigationIcon = {
                        IconButton(onClick = { openedCollectionId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
        ) { innerPadding ->
            val files = collectionDetail?.files.orEmpty()
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No hay archivos en esta colección todavía.\nUsa el menú (⋮) de cualquier archivo para asignarlo aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(files, key = { it.fileUri }) { record ->
                        val item = record.toFileItem()
                        OrganizedFileRow(
                            record = record,
                            imageLoader = imageLoader,
                            trailingIcon = Icons.Filled.DeleteOutline,
                            trailingDescription = "Quitar de colección",
                            onClick = { onOpenFile(item) },
                            onTrailingClick = {
                                viewModel.setFileInCollection(item, openedCollectionId!!, false)
                            },
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Organizar", fontWeight = FontWeight.Bold) },
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_collection_fab"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Nueva colección")
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                ) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Colecciones (${collections.size})") },
                            icon = { Icon(Icons.Filled.CollectionsBookmark, null) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Favoritos (${favorites.size})") },
                            icon = { Icon(Icons.Filled.Favorite, null) },
                        )
                    }

                    if (selectedTab == 0) {
                        if (collections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.CollectionsBookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Text("Crea colecciones personalizadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Agrupa videos, PDFs, apuntes o música de distintas carpetas en un solo lugar.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(collections, key = { it.collection.id }) { entry ->
                                    CollectionCard(
                                        entry = entry,
                                        onClick = { openedCollectionId = entry.collection.id },
                                        onDelete = { viewModel.deleteCollection(entry.collection.id) },
                                    )
                                }
                            }
                        }
                    } else {
                        if (favorites.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Text("Sin favoritos aún", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Marca tus videos o documentos importantes con el corazón para acceder al instante.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(favorites, key = { it.fileUri }) { record ->
                                    OrganizedFileRow(
                                        record = record,
                                        imageLoader = imageLoader,
                                        trailingIcon = Icons.Filled.Favorite,
                                        trailingTint = Color(0xFFDC2626),
                                        trailingDescription = "Quitar de favoritos",
                                        onClick = { onOpenFile(record.toFileItem()) },
                                        onTrailingClick = { viewModel.toggleFavoriteByUri(record) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorIdx ->
                viewModel.createCollection(name, colorIdx)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CollectionCard(
    entry: CollectionWithCount,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val swatch = CollectionPalette[entry.collection.colorIndex % CollectionPalette.size]
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = swatch.copy(alpha = 0.16f),
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CollectionsBookmark, null, tint = swatch)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.collection.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.itemCount} archivo(s) organizado(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar colección")
            }
        }
    }
}

@Composable
private fun OrganizedFileRow(
    record: FileRecordEntity,
    imageLoader: coil.ImageLoader,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingDescription: String,
    trailingTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    onTrailingClick: () -> Unit,
) {
    val item = record.toFileItem()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileThumbnailBox(item = item, imageLoader = imageLoader, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${item.category.label} • ${Formatters.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onTrailingClick,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(trailingIcon, contentDescription = trailingDescription, tint = trailingTint)
            }
        }
    }
}
