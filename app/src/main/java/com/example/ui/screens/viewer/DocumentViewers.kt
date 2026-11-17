package com.example.ui.screens.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FileCategory
import com.example.data.repository.FileExplorerRepository
import com.example.ui.components.iconVector
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PlusJakartaSans
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun ImageViewerView(
    uriString: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    offset = if (newScale > 1f) {
                        offset + pan
                    } else {
                        Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Uri.parse(uriString),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) {
            Text(
                text = "Pellizca o toca dos veces para hacer zoom",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun PdfViewerView(
    uriString: String,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderMutex = remember { Mutex() }
    var pageCount by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val rendererHolder = remember(uriString) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
            } else {
                loadError = "No se pudo abrir el archivo PDF"
            }
        } catch (e: Exception) {
            loadError = e.localizedMessage ?: "Error al abrir PDF"
        }
        Pair(pfd, renderer)
    }

    DisposableEffect(rendererHolder) {
        onDispose {
            runCatching { rendererHolder.second?.close() }
            runCatching { rendererHolder.first?.close() }
        }
    }

    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (loadError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(loadError.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = onOpenExternal) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir con visor externo")
                }
            }
        } else if (pageCount == 0) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val renderer = rendererHolder.second
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(pageCount) { pageIndex ->
                    val bitmap by produceState<Bitmap?>(initialValue = null, pageIndex, zoomScale) {
                        value = withContext(Dispatchers.IO) {
                            renderMutex.withLock {
                                renderer?.let { r ->
                                    r.openPage(pageIndex).use { page ->
                                        val width = (1080 * zoomScale).toInt().coerceIn(540, 2200)
                                        val height = ((page.height.toFloat() / page.width.toFloat()) * width).toInt().coerceAtLeast(100)
                                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        bmp.eraseColor(android.graphics.Color.WHITE)
                                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        bmp
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "Página ${pageIndex + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }

            // Bottom floating PDF toolbar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Alejar")
                    }
                    Text(
                        text = "Pág. $currentPage / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2f) },
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Acercar")
                    }
                }
            }
        }
    }
}

@Composable
fun TextDocumentViewerView(
    uriString: String,
    repository: FileExplorerRepository,
    onSavedMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var isTruncated by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useMono by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        isLoading = true
        repository.readTextContent(uriString)
            .onSuccess { (text, truncated) ->
                content = text
                isTruncated = truncated
                isLoading = false
            }
            .onFailure { err ->
                errorMessage = err.localizedMessage ?: "No se pudo leer el documento"
                isLoading = false
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Reader toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { useMono = !useMono }) {
                        Icon(Icons.Filled.FontDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (useMono) "Mono" else "Lectura")
                    }
                }

                if (!isTruncated && errorMessage == null && !isLoading) {
                    if (isEditing) {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.writeTextContent(uriString, content)
                                        .onSuccess {
                                            isEditing = false
                                            onSavedMessage("Documento guardado")
                                        }
                                        .onFailure { err ->
                                            onSavedMessage(err.localizedMessage ?: "Error al guardar")
                                        }
                                }
                            },
                            modifier = Modifier.testTag("save_document_button"),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.testTag("edit_document_button"),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Editar")
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        } else if (isEditing) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontFamily = if (useMono) JetBrainsMono else PlusJakartaSans,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("document_editor_field"),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                if (isTruncated) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Text(
                            text = "Mostrando los primeros 512 KB del archivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Text(
                    text = content.ifEmpty { "(Documento vacío — pulsa Editar para escribir)" },
                    style = TextStyle(
                        fontFamily = if (useMono) JetBrainsMono else PlusJakartaSans,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
        }
    }
}

@Composable
fun ExternalDocumentFallbackView(
    title: String,
    mimeType: String,
    category: FileCategory,
    sizeBytes: Long,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = category.accentColor()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = accent.copy(alpha = 0.14f),
            modifier = Modifier.size(110.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = category.iconVector(),
                    contentDescription = category.label,
                    tint = accent,
                    modifier = Modifier.size(54.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${category.label} • ${Formatters.formatFileSize(sizeBytes)}\n$mimeType",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onOpenExternal,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_external_button"),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Abrir con aplicación instalada")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Compartir archivo")
        }
    }
}
