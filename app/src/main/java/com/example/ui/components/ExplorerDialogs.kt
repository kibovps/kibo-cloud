package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.data.local.CollectionWithCount
import com.example.data.model.FileItem
import com.example.ui.theme.CollectionPalette
import com.example.util.Formatters

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmLabel: String = "Confirmar",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_text_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty(),
                modifier = Modifier.testTag("dialog_confirm_button"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun CreateDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (fileName: String, initialContent: String) -> Unit,
) {
    var name by remember { mutableStateOf("Nota.md") }
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo documento", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (.txt, .md, .json)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido inicial (opcional)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), content) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text("Crear y abrir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    itemName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val message = if (count == 1 && itemName != null) {
        "¿Seguro que deseas eliminar \"$itemName\"? Esta acción no se puede deshacer."
    } else {
        "¿Seguro que deseas eliminar $count elementos seleccionados? Esta acción no se puede deshacer."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar eliminación", style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIndex: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva colección", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la colección") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_name_input"),
                )
                Text("Color distintivo", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CollectionPalette.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selectedColor == index) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = index },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("create_collection_confirm"),
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenExternal: () -> Unit,
    onToggleFavorite: () -> Unit,
    onManageCollections: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (item.isDirectory) "Carpeta" else "${item.category.label} • ${Formatters.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (!item.isDirectory) {
                ListItem(
                    headlineContent = { Text("Abrir en Kibo VPS Cloud") },
                    leadingContent = { Icon(item.category.iconVector(), null, tint = item.category.accentColor()) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onOpen()
                    },
                )
                ListItem(
                    headlineContent = { Text("Abrir con otra aplicación") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onOpenExternal()
                    },
                )
                ListItem(
                    headlineContent = {
                        Text(if (item.isFavorite) "Quitar de favoritos" else "Añadir a favoritos")
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (item.isFavorite) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onToggleFavorite()
                    },
                )
                ListItem(
                    headlineContent = { Text("Organizar en colecciones") },
                    leadingContent = { Icon(Icons.Filled.CollectionsBookmark, null) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onManageCollections()
                    },
                )
                ListItem(
                    headlineContent = { Text("Compartir") },
                    leadingContent = { Icon(Icons.Filled.Share, null) },
                    modifier = Modifier.clickable {
                        onDismiss()
                        onShare()
                    },
                )
            }
            ListItem(
                headlineContent = { Text("Mover a...") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onMove()
                },
            )
            ListItem(
                headlineContent = { Text("Copiar a...") },
                leadingContent = { Icon(Icons.Filled.ContentCopy, null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onCopy()
                },
            )
            ListItem(
                headlineContent = { Text("Renombrar") },
                leadingContent = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onRename()
                },
            )
            ListItem(
                headlineContent = { Text("Propiedades") },
                leadingContent = { Icon(Icons.Filled.Info, null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onDetails()
                },
            )
            ListItem(
                headlineContent = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onDelete()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionAssignmentSheet(
    item: FileItem,
    collections: List<CollectionWithCount>,
    onDismiss: () -> Unit,
    onToggleCollection: (collectionId: Long, assign: Boolean) -> Unit,
    onCreateNewCollection: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Organizar en colecciones", style = MaterialTheme.typography.titleLarge)
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onCreateNewCollection) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nueva")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (collections.isEmpty()) {
                Text(
                    "Aún no tienes colecciones. Crea una para agrupar tus videos, música y documentos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(collections, key = { it.collection.id }) { entry ->
                        val isAssigned = entry.collection.id in item.collectionIds
                        val swatch = CollectionPalette[entry.collection.colorIndex % CollectionPalette.size]
                        ListItem(
                            headlineContent = { Text(entry.collection.name) },
                            supportingContent = { Text("${entry.itemCount} archivo(s)") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(swatch),
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isAssigned,
                                    onCheckedChange = { checked ->
                                        onToggleCollection(entry.collection.id, checked)
                                    },
                                )
                            },
                            modifier = Modifier.clickable {
                                onToggleCollection(entry.collection.id, !isAssigned)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalles del archivo", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Nombre", item.name)
                DetailRow("Tipo", item.category.label)
                DetailRow("MIME", item.mimeType)
                if (!item.isDirectory) {
                    DetailRow("Tamaño", "${Formatters.formatFileSize(item.sizeBytes)} (${item.sizeBytes} bytes)")
                }
                DetailRow("Modificado", Formatters.formatDate(item.lastModified))
                DetailRow("Origen", if (item.source.name == "VAULT") "Bóveda Local" else "Carpeta Vinculada (SAF)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
