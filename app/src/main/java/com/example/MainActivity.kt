package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.model.FileItem
import com.example.data.model.FileSource
import com.example.data.model.FileTypeClassifier
import com.example.ui.navigation.ArchivaApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val externalItem = extractExternalFileItem(intent)
        setContent {
            ArchivaApp(initialExternalFile = externalItem)
        }
    }

    private fun extractExternalFileItem(intent: Intent?): FileItem? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri: Uri = intent.data ?: return null
        var name = uri.lastPathSegment ?: "Archivo"
        var size = 0L
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        val mime = FileTypeClassifier.inferMimeType(name, intent.type ?: contentResolver.getType(uri))
        val category = FileTypeClassifier.classify(name, mime, false)
        return FileItem(
            id = uri.toString(),
            uriString = uri.toString(),
            name = name,
            isDirectory = false,
            sizeBytes = size,
            lastModified = System.currentTimeMillis(),
            mimeType = mime,
            extension = name.substringAfterLast('.', ""),
            category = category,
            source = FileSource.SAF,
            parentUriString = "",
        )
    }
}
