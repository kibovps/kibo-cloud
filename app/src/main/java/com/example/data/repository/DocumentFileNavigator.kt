package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.BreadcrumbSegment
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.FileTypeClassifier
import java.io.File

/**
 * Core DocumentFile navigation engine supporting both local File-backed directories
 * (via DocumentFile.fromFile) and Storage Access Framework trees (via DocumentFile.fromTreeUri).
 */
class DocumentFileNavigator(private val context: Context) {

    fun resolveDirectory(segment: BreadcrumbSegment): DocumentFile? {
        return resolveDirectory(
            rootUriString = segment.rootUriString,
            source = segment.source,
            pathSegments = segment.pathSegments,
            directUriString = segment.uriString,
        )
    }

    fun resolveDirectory(
        rootUriString: String,
        source: FileSource,
        pathSegments: List<String>,
        directUriString: String = rootUriString,
    ): DocumentFile? {
        return when (source) {
            FileSource.VAULT -> {
                val parsed = Uri.parse(directUriString)
                val file = File(parsed.path ?: return null)
                if (file.exists() && file.isDirectory) DocumentFile.fromFile(file) else null
            }
            FileSource.SAF -> {
                val rootTree = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString))
                    ?: return null
                var current: DocumentFile = rootTree
                for (segmentName in pathSegments) {
                    val child = current.findFile(segmentName)
                    if (child == null || !child.isDirectory) {
                        return null
                    }
                    current = child
                }
                current
            }
        }
    }

    fun resolveDocument(
        uriString: String,
        source: FileSource,
        parentSegment: BreadcrumbSegment? = null,
        itemName: String? = null,
    ): DocumentFile? {
        return when (source) {
            FileSource.VAULT -> {
                val path = Uri.parse(uriString).path ?: return null
                DocumentFile.fromFile(File(path))
            }
            FileSource.SAF -> {
                if (parentSegment != null && !itemName.isNullOrBlank()) {
                    resolveDirectory(parentSegment)?.findFile(itemName)?.let { return it }
                }
                val parsed = Uri.parse(uriString)
                if (DocumentsContract.isTreeUri(parsed)) {
                    DocumentFile.fromTreeUri(context, parsed)
                } else {
                    DocumentFile.fromSingleUri(context, parsed)
                }
            }
        }
    }

    fun listFilesInDirectory(
        directory: DocumentFile,
        source: FileSource,
    ): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
            throw IllegalStateException("La carpeta no existe o no tiene permisos de lectura")
        }

        val parentUri = directory.uri.toString()
        return directory.listFiles().mapNotNull { doc ->
            val name = doc.name ?: return@mapNotNull null
            val isDir = doc.isDirectory
            val rawMime = doc.type
            val mime = if (isDir) {
                DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                FileTypeClassifier.inferMimeType(name, rawMime)
            }
            val ext = if (isDir) "" else name.substringAfterLast('.', "")
            val uriStr = doc.uri.toString()
            FileItem(
                id = uriStr,
                uriString = uriStr,
                name = name,
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else doc.length(),
                lastModified = doc.lastModified(),
                mimeType = mime,
                extension = ext,
                category = FileTypeClassifier.classify(name, mime, isDir),
                source = source,
                parentUriString = parentUri,
                canWrite = doc.canWrite(),
            )
        }
    }

    fun listFiles(segment: BreadcrumbSegment): List<FileItem> {
        val directory = resolveDirectory(segment)
            ?: throw IllegalStateException("No se pudo abrir el directorio seleccionado")
        return listFilesInDirectory(directory, segment.source)
    }

    fun createSubdirectory(parentSegment: BreadcrumbSegment, name: String): DocumentFile {
        val parent = resolveDirectory(parentSegment)
            ?: throw IllegalStateException("Carpeta padre no accesible")
        if (parent.findFile(name) != null) {
            throw IllegalStateException("Ya existe una carpeta con ese nombre")
        }
        return parent.createDirectory(name)
            ?: throw IllegalStateException("No se pudo crear la carpeta \"$name\"")
    }

    fun createFile(
        parentSegment: BreadcrumbSegment,
        mimeType: String,
        displayName: String,
    ): DocumentFile {
        val parent = resolveDirectory(parentSegment)
            ?: throw IllegalStateException("Carpeta padre no accesible")
        return parent.createFile(mimeType, displayName)
            ?: throw IllegalStateException("No se pudo crear el archivo \"$displayName\"")
    }
}
