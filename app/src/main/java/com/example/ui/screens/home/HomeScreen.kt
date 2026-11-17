package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.FileRecordEntity
import com.example.data.local.LinkedFolderEntity
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.ThemePreference
import com.example.data.repository.VaultStats
import com.example.ui.components.FileThumbnailBox
import com.example.ui.components.iconVector
import com.example.ui.components.rememberMediaImageLoader
import com.example.ui.theme.CollectionPalette
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.toFileItem
import com.example.util.Formatters

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenVault: (FileCategory?) -> Unit,
    onOpenLinkedFolder: (LinkedFolderEntity) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onPickMedia: () -> Unit,
    onPickDocuments: () -> Unit,
    onLinkDeviceFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats by viewModel.vaultStats.collectAsStateWithLifecycle()
    val linkedFolders by viewModel.linkedFolders.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val themePref by viewModel.themePreference.collectAsStateWithLifecycle()
    val imageLoader = rememberMediaImageLoader()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Editorial Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Kibo VPS Cloud",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Tu centro de archivos y multimedia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        IconButton(
                            onClick = {
                                val next = when (themePref) {
                                    ThemePreference.SYSTEM -> ThemePreference.LIGHT
                                    ThemePreference.LIGHT -> ThemePreference.DARK
                                    ThemePreference.DARK -> ThemePreference.SYSTEM
                                }
                                viewModel.setThemePreference(next)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_toggle_button"),
                        ) {
                            Icon(
                                imageVector = when (themePref) {
                                    ThemePreference.SYSTEM -> Icons.Filled.BrightnessAuto
                                    ThemePreference.LIGHT -> Icons.Filled.LightMode
                                    ThemePreference.DARK -> Icons.Filled.DarkMode
                                },
                                contentDescription = "Tema: ${themePref.label}",
                            )
                        }
                    }
                }
            }

            // Hero Vault Card
            item {
                VaultHeroCard(
                    stats = stats,
                    onExploreVault = { onOpenVault(null) },
                    onImportMedia = onPickMedia,
                )
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Acciones rápidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickActionTile(
                            title = "Multimedia",
                            subtitle = "Fotos y videos",
                            icon = Icons.Filled.PermMedia,
                            accent = FileCategory.VIDEO.accentColor(),
                            onClick = onPickMedia,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_import_media_btn"),
                        )
                        QuickActionTile(
                            title = "Documentos",
                            subtitle = "PDF, audio, otros",
                            icon = Icons.Filled.FileUpload,
                            accent = FileCategory.PDF.accentColor(),
                            onClick = onPickDocuments,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_import_docs_btn"),
                        )
                        QuickActionTile(
                            title = "Vincular",
                            subtitle = "Carpeta externa",
                            icon = Icons.Filled.AddLink,
                            accent = FileCategory.FOLDER.accentColor(),
                            onClick = onLinkDeviceFolder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_link_folder_btn"),
                        )
                    }
                }
            }

            // Category Quick Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Explorar por formato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val categories = listOf(
                        FileCategory.VIDEO,
                        FileCategory.AUDIO,
                        FileCategory.IMAGE,
                        FileCategory.PDF,
                        FileCategory.TEXT,
                        FileCategory.DOCUMENT,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categories) { category ->
                            val count = stats.categoryCounts[category] ?: 0
                            CategoryChipCard(
                                category = category,
                                count = count,
                                onClick = { onOpenVault(category) },
                            )
                        }
                    }
                }
            }

            // Linked Device Folders (SAF)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Carpetas del dispositivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        FilledTonalButton(onClick = onLinkDeviceFolder) {
                            Icon(Icons.Filled.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Vincular")
                        }
                    }

                    if (linkedFolders.isEmpty()) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLinkDeviceFolder() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = FileCategory.FOLDER.accentColor().copy(alpha = 0.14f),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.AddLink,
                                            contentDescription = null,
                                            tint = FileCategory.FOLDER.accentColor(),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Conecta Descargas, Documentos o Videos",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Toca aquí para elegir cualquier carpeta del teléfono y explorarla sin límites.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            linkedFolders.forEach { folder ->
                                LinkedFolderCard(
                                    folder = folder,
                                    onClick = { onOpenLinkedFolder(folder) },
                                    onUnlink = { viewModel.unlinkSafFolder(folder.uriString) },
                                )
                            }
                        }
                    }
                }
            }

            // Recent Files
            if (recentFiles.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Abiertos recientemente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentFiles.take(8).forEach { record ->
                                RecentFileRow(
                                    record = record,
                                    imageLoader = imageLoader,
                                    onClick = { onOpenFile(record.toFileItem()) },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun VaultHeroCard(
    stats: VaultStats,
    onExploreVault: () -> Unit,
    onImportMedia: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = "BÓVEDA ORGANIZADA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${stats.totalFiles} archivos • ${stats.totalFolders} carpetas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Espacio usado: ${Formatters.formatFileSize(stats.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.img_empty_vault),
                        contentDescription = "Ilustración de bóveda de archivos",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onExploreVault,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_vault_button"),
                    ) {
                        Icon(Icons.Filled.FolderSpecial, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir explorador")
                    }
                    OutlinedButton(
                        onClick = onImportMedia,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Importar")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CategoryChipCard(
    category: FileCategory,
    count: Int,
    onClick: () -> Unit,
) {
    val accent = category.accentColor()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(category.iconVector(), null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(category.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "$count en bóveda",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LinkedFolderCard(
    folder: LinkedFolderEntity,
    onClick: () -> Unit,
    onUnlink: () -> Unit,
) {
    val swatch = CollectionPalette[folder.colorIndex % CollectionPalette.size]
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = swatch.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(FileCategory.FOLDER.iconVector(), null, tint = swatch)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Carpeta externa vinculada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onUnlink,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Desvincular carpeta",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentFileRow(
    record: FileRecordEntity,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit,
) {
    val item = record.toFileItem()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
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
            FileThumbnailBox(item = item, imageLoader = imageLoader, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.category.label} • ${Formatters.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
