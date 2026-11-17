package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ColorArchive
import com.example.ui.theme.ColorAudio
import com.example.ui.theme.ColorDocument
import com.example.ui.theme.ColorFolder
import com.example.ui.theme.ColorImage
import com.example.ui.theme.ColorOther
import com.example.ui.theme.ColorPdf
import com.example.ui.theme.ColorText
import com.example.ui.theme.ColorVideo

enum class FileCategory(val label: String) {
    FOLDER("Carpetas"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    IMAGE("Imágenes"),
    PDF("PDF"),
    TEXT("Notas / Texto"),
    DOCUMENT("Documentos"),
    ARCHIVE("Comprimidos"),
    OTHER("Otros");

    fun accentColor(): Color = when (this) {
        FOLDER -> ColorFolder
        VIDEO -> ColorVideo
        AUDIO -> ColorAudio
        IMAGE -> ColorImage
        PDF -> ColorPdf
        TEXT -> ColorText
        DOCUMENT -> ColorDocument
        ARCHIVE -> ColorArchive
        OTHER -> ColorOther
    }
}

enum class FileSource {
    VAULT,
    SAF,
}

enum class ViewMode {
    GRID,
    LIST,
}

enum class SortOption(val label: String) {
    NAME_ASC("Nombre (A-Z)"),
    NAME_DESC("Nombre (Z-A)"),
    DATE_DESC("Más recientes"),
    SIZE_DESC("Mayor tamaño"),
    TYPE("Tipo de archivo"),
}

enum class ThemePreference(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

data class FileItem(
    val id: String,
    val uriString: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String,
    val extension: String,
    val category: FileCategory,
    val source: FileSource,
    val parentUriString: String,
    val canWrite: Boolean = true,
    val isFavorite: Boolean = false,
    val collectionIds: List<Long> = emptyList(),
)

data class BreadcrumbSegment(
    val name: String,
    val uriString: String,
    val source: FileSource,
    val rootUriString: String = uriString,
    val pathSegments: List<String> = emptyList(),
)

data class StorageLocation(
    val name: String,
    val subtitle: String,
    val uriString: String,
    val source: FileSource,
)

data class PendingTransfer(
    val items: List<FileItem>,
    val isMove: Boolean,
)

object FileTypeClassifier {
    private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "flv", "ts")
    private val audioExtensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus", "wma")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
    private val textExtensions = setOf(
        "txt", "md", "markdown", "json", "xml", "csv", "log", "kt", "java",
        "py", "js", "ts", "html", "css", "yaml", "yml", "ini", "conf", "sh", "sql"
    )
    private val documentExtensions = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "epub")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    fun classify(name: String, mimeType: String?, isDirectory: Boolean): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.lowercase().orEmpty()

        return when {
            mime == "application/pdf" || ext == "pdf" -> FileCategory.PDF
            mime.startsWith("video/") || ext in videoExtensions -> FileCategory.VIDEO
            mime.startsWith("audio/") || ext in audioExtensions -> FileCategory.AUDIO
            mime.startsWith("image/") || ext in imageExtensions -> FileCategory.IMAGE
            mime.startsWith("text/") || mime == "application/json" || ext in textExtensions -> FileCategory.TEXT
            ext in documentExtensions || mime.contains("word") || mime.contains("spreadsheet") || mime.contains("presentation") -> FileCategory.DOCUMENT
            ext in archiveExtensions || mime.contains("zip") || mime.contains("compressed") -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    fun inferMimeType(name: String, providedMime: String?): String {
        if (!providedMime.isNullOrBlank() && providedMime != "application/octet-stream") {
            return providedMime
        }
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt", "log" -> "text/plain"
            "md", "markdown" -> "text/markdown"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "html" -> "text/html"
            "xml" -> "text/xml"
            "zip" -> "application/zip"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "application/octet-stream"
        }
    }
}
