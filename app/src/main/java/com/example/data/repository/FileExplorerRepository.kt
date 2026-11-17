package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.ArchivaDao
import com.example.data.local.CollectionEntity
import com.example.data.local.CollectionWithCount
import com.example.data.local.CollectionWithFiles
import com.example.data.local.FileCollectionCrossRef
import com.example.data.local.FileRecordEntity
import com.example.data.local.LinkedFolderEntity
import com.example.data.model.BreadcrumbSegment
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.FileTypeClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class VaultStats(
    val totalFiles: Int = 0,
    val totalFolders: Int = 0,
    val totalBytes: Long = 0L,
    val categoryCounts: Map<FileCategory, Int> = emptyMap(),
)

class FileExplorerRepository(
    private val context: Context,
    private val dao: ArchivaDao,
) {
    val vaultRootDir: File by lazy {
        File(context.filesDir, "ArchivaVault").apply {
            if (!exists()) {
                mkdirs()
                // Create clean, empty organizational directories inside the user's private vault
                listOf("Documentos", "Videos", "Música", "Imágenes", "Proyectos").forEach { sub ->
                    File(this, sub).mkdirs()
                }
            }
        }
    }

    val navigator = DocumentFileNavigator(context)

    val vaultRootUri: String
        get() = Uri.fromFile(vaultRootDir).toString()

    val externalAppStorageUri: String?
        get() = context.getExternalFilesDir(null)?.let { dir ->
            if (!dir.exists()) dir.mkdirs()
            Uri.fromFile(dir).toString()
        }

    // Reactive Room streams
    val linkedFolders: Flow<List<LinkedFolderEntity>> = dao.observeLinkedFolders()
    val collectionsWithCount: Flow<List<CollectionWithCount>> = dao.observeCollectionsWithCount()
    val favorites: Flow<List<FileRecordEntity>> = dao.observeFavorites()
    val recentFiles: Flow<List<FileRecordEntity>> = dao.observeRecentFiles()
    val allFileRecords: Flow<List<FileRecordEntity>> = dao.observeAllFileRecords()
    val allCrossRefs: Flow<List<FileCollectionCrossRef>> = dao.observeAllCrossRefs()

    fun observeCollectionWithFiles(collectionId: Long): Flow<CollectionWithFiles?> =
        dao.observeCollectionWithFiles(collectionId)

    suspend fun listDirectory(segment: BreadcrumbSegment): Result<List<FileItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                navigator.listFiles(segment)
            }
        }

    suspend fun listDirectory(
        directoryUriString: String,
        source: FileSource,
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val segment = BreadcrumbSegment(
                name = "",
                uriString = directoryUriString,
                source = source,
                rootUriString = directoryUriString,
                pathSegments = emptyList(),
            )
            navigator.listFiles(segment)
        }
    }

    suspend fun computeVaultStats(): VaultStats = withContext(Dispatchers.IO) {
        var filesCount = 0
        var foldersCount = 0
        var bytes = 0L
        val counts = mutableMapOf<FileCategory, Int>()

        fun walk(dir: File) {
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    foldersCount++
                    walk(child)
                } else {
                    filesCount++
                    bytes += child.length()
                    val cat = FileTypeClassifier.classify(child.name, null, false)
                    counts[cat] = (counts[cat] ?: 0) + 1
                }
            }
        }
        walk(vaultRootDir)
        VaultStats(filesCount, foldersCount, bytes, counts)
    }

    suspend fun searchVaultRecursive(query: String): List<FileItem> = withContext(Dispatchers.IO) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return@withContext emptyList()
        val results = mutableListOf<FileItem>()

        fun walk(dir: File) {
            val parentUri = Uri.fromFile(dir).toString()
            dir.listFiles()?.forEach { child ->
                if (child.name.lowercase().contains(normalized)) {
                    val isDir = child.isDirectory
                    val mime = if (isDir) "vnd.android.document/directory" else FileTypeClassifier.inferMimeType(child.name, null)
                    val ext = if (isDir) "" else child.name.substringAfterLast('.', "")
                    results += FileItem(
                        id = Uri.fromFile(child).toString(),
                        uriString = Uri.fromFile(child).toString(),
                        name = child.name,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else child.length(),
                        lastModified = child.lastModified(),
                        mimeType = mime,
                        extension = ext,
                        category = FileTypeClassifier.classify(child.name, mime, isDir),
                        source = FileSource.VAULT,
                        parentUriString = parentUri,
                    )
                }
                if (child.isDirectory) walk(child)
            }
        }
        walk(vaultRootDir)
        results
    }

    suspend fun createFolder(parentSegment: BreadcrumbSegment, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = name.trim().replace('/', '_')
                require(safeName.isNotEmpty()) { "El nombre no puede estar vacío" }
                navigator.createSubdirectory(parentSegment, safeName)
                Unit
            }
        }

    suspend fun createFolder(parentUriString: String, source: FileSource, name: String): Result<Unit> =
        createFolder(
            BreadcrumbSegment(
                name = "",
                uriString = parentUriString,
                source = source,
                rootUriString = parentUriString,
            ),
            name,
        )

    suspend fun createTextDocument(
        parentSegment: BreadcrumbSegment,
        fileName: String,
        content: String,
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = fileName.trim().replace('/', '_').let {
                if (it.contains('.')) it else "$it.txt"
            }
            require(cleanName.isNotEmpty()) { "El nombre no puede estar vacío" }
            val mime = FileTypeClassifier.inferMimeType(cleanName, null)
            when (parentSegment.source) {
                FileSource.VAULT -> {
                    val parent = resolveVaultFile(parentSegment.uriString)
                    val target = uniqueVaultFile(parent, cleanName)
                    target.writeText(content)
                    val uriStr = Uri.fromFile(target).toString()
                    FileItem(
                        id = uriStr,
                        uriString = uriStr,
                        name = target.name,
                        isDirectory = false,
                        sizeBytes = target.length(),
                        lastModified = target.lastModified(),
                        mimeType = mime,
                        extension = target.extension,
                        category = FileTypeClassifier.classify(target.name, mime, false),
                        source = FileSource.VAULT,
                        parentUriString = parentSegment.uriString,
                    )
                }
                FileSource.SAF -> {
                    val created = navigator.createFile(parentSegment, mime, cleanName)
                    context.contentResolver.openOutputStream(created.uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    } ?: error("No se pudo escribir en el archivo")
                    val uriStr = created.uri.toString()
                    FileItem(
                        id = uriStr,
                        uriString = uriStr,
                        name = created.name ?: cleanName,
                        isDirectory = false,
                        sizeBytes = created.length(),
                        lastModified = created.lastModified(),
                        mimeType = mime,
                        extension = cleanName.substringAfterLast('.', ""),
                        category = FileTypeClassifier.classify(cleanName, mime, false),
                        source = FileSource.SAF,
                        parentUriString = parentSegment.uriString,
                    )
                }
            }
        }
    }

    suspend fun createTextDocument(
        parentUriString: String,
        source: FileSource,
        fileName: String,
        content: String,
    ): Result<FileItem> = createTextDocument(
        BreadcrumbSegment(name = "", uriString = parentUriString, source = source),
        fileName,
        content,
    )

    suspend fun renameItem(
        item: FileItem,
        newName: String,
        parentSegment: BreadcrumbSegment? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = newName.trim().replace('/', '_')
            require(clean.isNotEmpty()) { "Nombre inválido" }
            when (item.source) {
                FileSource.VAULT -> {
                    val file = resolveVaultFile(item.uriString)
                    val target = File(file.parentFile, clean)
                    if (target.exists()) error("Ya existe un elemento con ese nombre")
                    if (!file.renameTo(target)) error("No se pudo renombrar")
                }
                FileSource.SAF -> {
                    val doc = navigator.resolveDocument(item.uriString, item.source, parentSegment, item.name)
                        ?: error("Elemento no encontrado")
                    if (!doc.renameTo(clean)) error("El proveedor de almacenamiento no permitió renombrar")
                }
            }
        }
    }

    suspend fun deleteItems(
        items: List<FileItem>,
        parentSegment: BreadcrumbSegment? = null,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var deleted = 0
            for (item in items) {
                val ok = when (item.source) {
                    FileSource.VAULT -> resolveVaultFile(item.uriString).deleteRecursively()
                    FileSource.SAF -> {
                        val doc = navigator.resolveDocument(item.uriString, item.source, parentSegment, item.name)
                        doc?.delete() == true
                    }
                }
                if (ok) {
                    deleted++
                    dao.deleteFileRecord(item.uriString)
                    dao.clearFileCollections(item.uriString)
                }
            }
            deleted
        }
    }

    suspend fun importUris(
        uris: List<Uri>,
        targetSegment: BreadcrumbSegment,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var count = 0
            val targetDocDir = navigator.resolveDirectory(targetSegment)
                ?: error("Carpeta de destino no accesible")
            for (uri in uris) {
                val (displayName, mimeType) = queryUriMetadata(uri)
                when (targetSegment.source) {
                    FileSource.VAULT -> {
                        val parentDir = resolveVaultFile(targetSegment.uriString)
                        val destFile = uniqueVaultFile(parentDir, displayName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        } ?: error("No se pudo leer uno de los archivos")
                        count++
                    }
                    FileSource.SAF -> {
                        val created = targetDocDir.createFile(mimeType, displayName)
                            ?: error("No se pudo crear $displayName en el destino")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            context.contentResolver.openOutputStream(created.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        count++
                    }
                }
            }
            count
        }
    }

    suspend fun transferItems(
        items: List<FileItem>,
        targetSegment: BreadcrumbSegment,
        isMove: Boolean,
        sourceParentSegment: BreadcrumbSegment? = null,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var transferred = 0
            val targetDocDir = navigator.resolveDirectory(targetSegment)
                ?: error("Carpeta destino inaccesible")
            for (item in items) {
                if (item.parentUriString == targetSegment.uriString && isMove) continue
                if (item.uriString == targetSegment.uriString) continue

                if (item.source == FileSource.VAULT && targetSegment.source == FileSource.VAULT) {
                    val src = resolveVaultFile(item.uriString)
                    val destParent = resolveVaultFile(targetSegment.uriString)
                    if (isMove && src.isDirectory && destParent.canonicalPath.startsWith(src.canonicalPath)) {
                        error("No puedes mover una carpeta dentro de sí misma")
                    }
                    val dest = uniqueVaultFile(destParent, src.name)
                    if (isMove) {
                        if (!src.renameTo(dest)) {
                            src.copyRecursively(dest, overwrite = false)
                            src.deleteRecursively()
                        }
                    } else {
                        src.copyRecursively(dest, overwrite = false)
                    }
                    transferred++
                } else {
                    if (item.isDirectory) {
                        val srcDoc = navigator.resolveDocument(item.uriString, item.source, sourceParentSegment, item.name)
                            ?: error("Carpeta origen inaccesible")
                        copyDocumentTree(srcDoc, targetDocDir, targetSegment.source)
                        if (isMove) deleteItems(listOf(item), sourceParentSegment)
                        transferred++
                    } else {
                        copySingleDocument(item.uriString, item.name, item.mimeType, targetDocDir, targetSegment.source)
                        if (isMove) deleteItems(listOf(item), sourceParentSegment)
                        transferred++
                    }
                }
            }
            transferred
        }
    }

    private fun copyDocumentTree(
        sourceDir: DocumentFile,
        targetParentDoc: DocumentFile,
        targetSource: FileSource,
    ) {
        val dirName = sourceDir.name ?: "Carpeta"
        val createdDir = when (targetSource) {
            FileSource.VAULT -> {
                val parentFile = resolveVaultFile(targetParentDoc.uri.toString())
                val dest = uniqueVaultFile(parentFile, dirName)
                dest.mkdirs()
                DocumentFile.fromFile(dest)
            }
            FileSource.SAF -> {
                targetParentDoc.createDirectory(dirName)
                    ?: error("No se pudo crear carpeta $dirName")
            }
        }
        for (child in sourceDir.listFiles()) {
            val childName = child.name ?: continue
            if (child.isDirectory) {
                copyDocumentTree(child, createdDir, targetSource)
            } else {
                val mime = FileTypeClassifier.inferMimeType(childName, child.type)
                copySingleDocument(child.uri.toString(), childName, mime, createdDir, targetSource)
            }
        }
    }

    private fun copySingleDocument(
        srcUriString: String,
        fileName: String,
        mimeType: String,
        targetParentDoc: DocumentFile,
        targetSource: FileSource,
    ) {
        val srcUri = Uri.parse(srcUriString)
        when (targetSource) {
            FileSource.VAULT -> {
                val destFile = uniqueVaultFile(resolveVaultFile(targetParentDoc.uri.toString()), fileName)
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }
            FileSource.SAF -> {
                val created = targetParentDoc.createFile(mimeType, fileName)
                    ?: error("No se pudo copiar $fileName")
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    context.contentResolver.openOutputStream(created.uri)?.use { output -> input.copyTo(output) }
                }
            }
        }
    }

    suspend fun linkSafFolder(treeUri: Uri): Result<LinkedFolderEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            } catch (_: SecurityException) {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val name = doc?.name?.takeIf { it.isNotBlank() } ?: "Carpeta externa"
            val entity = LinkedFolderEntity(
                uriString = treeUri.toString(),
                displayName = name,
                colorIndex = (System.currentTimeMillis() % 8).toInt(),
            )
            dao.upsertLinkedFolder(entity)
            entity
        }
    }

    suspend fun unlinkSafFolder(uriString: String) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        dao.deleteLinkedFolder(uriString)
    }

    // Room metadata & collections operations
    suspend fun markFileOpened(item: FileItem) = withContext(Dispatchers.IO) {
        val existing = dao.getFileRecord(item.uriString)
        dao.upsertFileRecord(
            FileRecordEntity(
                fileUri = item.uriString,
                fileName = item.name,
                mimeType = item.mimeType,
                categoryName = item.category.name,
                sourceName = item.source.name,
                parentUri = item.parentUriString,
                sizeBytes = item.sizeBytes,
                isFavorite = existing?.isFavorite ?: item.isFavorite,
                lastOpenedAt = System.currentTimeMillis(),
                playbackPositionMs = existing?.playbackPositionMs ?: 0L,
            )
        )
    }

    suspend fun toggleFavorite(item: FileItem) = withContext(Dispatchers.IO) {
        val existing = dao.getFileRecord(item.uriString)
        val newFav = !(existing?.isFavorite ?: item.isFavorite)
        if (existing != null) {
            dao.updateFavorite(item.uriString, newFav)
        } else {
            dao.upsertFileRecord(
                FileRecordEntity(
                    fileUri = item.uriString,
                    fileName = item.name,
                    mimeType = item.mimeType,
                    categoryName = item.category.name,
                    sourceName = item.source.name,
                    parentUri = item.parentUriString,
                    sizeBytes = item.sizeBytes,
                    isFavorite = newFav,
                    lastOpenedAt = 0L,
                    playbackPositionMs = 0L,
                )
            )
        }
    }

    suspend fun savePlaybackPosition(fileUri: String, positionMs: Long) = withContext(Dispatchers.IO) {
        if (dao.getFileRecord(fileUri) != null) {
            dao.updatePlaybackPosition(fileUri, positionMs)
        }
    }

    suspend fun getSavedPlaybackPosition(fileUri: String): Long = withContext(Dispatchers.IO) {
        dao.getFileRecord(fileUri)?.playbackPositionMs ?: 0L
    }

    suspend fun createCollection(name: String, colorIndex: Int): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = name.trim()
            require(clean.isNotEmpty()) { "El nombre de la colección es obligatorio" }
            dao.insertCollection(CollectionEntity(name = clean, colorIndex = colorIndex))
        }
    }

    suspend fun deleteCollection(collectionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteCollection(collectionId)
    }

    suspend fun setFileInCollection(item: FileItem, collectionId: Long, inCollection: Boolean) =
        withContext(Dispatchers.IO) {
            if (inCollection) {
                if (dao.getFileRecord(item.uriString) == null) {
                    dao.upsertFileRecord(
                        FileRecordEntity(
                            fileUri = item.uriString,
                            fileName = item.name,
                            mimeType = item.mimeType,
                            categoryName = item.category.name,
                            sourceName = item.source.name,
                            parentUri = item.parentUriString,
                            sizeBytes = item.sizeBytes,
                            isFavorite = item.isFavorite,
                        )
                    )
                }
                dao.addFileToCollection(FileCollectionCrossRef(item.uriString, collectionId))
            } else {
                dao.removeFileFromCollection(item.uriString, collectionId)
            }
        }

    suspend fun readTextContent(uriString: String, maxBytes: Int = 512 * 1024): Result<Pair<String, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("No se pudo abrir el archivo")
                stream.use { input ->
                    val buffer = ByteArray(maxBytes + 1)
                    var totalRead = 0
                    while (totalRead < buffer.size) {
                        val read = input.read(buffer, totalRead, buffer.size - totalRead)
                        if (read <= 0) break
                        totalRead += read
                    }
                    val truncated = totalRead > maxBytes
                    val len = minOf(totalRead, maxBytes)
                    String(buffer, 0, len, Charsets.UTF_8) to truncated
                }
            }
        }

    suspend fun writeTextContent(uriString: String, newText: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    File(requireNotNull(uri.path)).writeText(newText, Charsets.UTF_8)
                } else {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(newText.toByteArray(Charsets.UTF_8))
                    } ?: error("Este archivo es de solo lectura")
                }
            }
        }

    private fun resolveVaultFile(uriString: String): File {
        val parsed = Uri.parse(uriString)
        val path = parsed.path ?: return vaultRootDir
        return File(path)
    }

    private fun uniqueVaultFile(parent: File, desiredName: String): File {
        var candidate = File(parent, desiredName)
        if (!candidate.exists()) return candidate
        val base = desiredName.substringBeforeLast('.', desiredName)
        val ext = desiredName.substringAfterLast('.', "")
        var index = 1
        while (candidate.exists()) {
            val nextName = if (ext.isNotEmpty() && desiredName.contains('.')) {
                "$base ($index).$ext"
            } else {
                "$base ($index)"
            }
            candidate = File(parent, nextName)
            index++
        }
        return candidate
    }

    private fun queryUriMetadata(uri: Uri): Pair<String, String> {
        var name = "archivo_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                }
            }
        }
        val rawMime = context.contentResolver.getType(uri)
        val mime = FileTypeClassifier.inferMimeType(name, rawMime)
        return name to mime
    }
}
