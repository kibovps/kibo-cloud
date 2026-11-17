package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.FileCategory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Formatters {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "—"
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun resolveShareableUri(context: Context, uriString: String): Uri {
        val parsed = Uri.parse(uriString)
        return if (parsed.scheme == "file" || parsed.scheme == null) {
            val file = File(parsed.path ?: uriString)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            parsed
        }
    }

    fun openWithExternalApp(context: Context, uriString: String, mimeType: String) {
        try {
            val shareableUri = resolveShareableUri(context, uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareableUri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_open_external)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_external_app), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: "Error al abrir archivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, uriString: String, mimeType: String, displayName: String) {
        try {
            val shareableUri = resolveShareableUri(context, uriString)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, shareableUri)
                putExtra(Intent.EXTRA_SUBJECT, displayName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: "Error al compartir", Toast.LENGTH_SHORT).show()
        }
    }

    fun categoryBadgeText(category: FileCategory, extension: String): String {
        return extension.uppercase().takeIf { it.isNotBlank() && it.length <= 5 }
            ?: when (category) {
                FileCategory.FOLDER -> "DIR"
                FileCategory.VIDEO -> "VID"
                FileCategory.AUDIO -> "AUD"
                FileCategory.IMAGE -> "IMG"
                FileCategory.PDF -> "PDF"
                FileCategory.TEXT -> "TXT"
                FileCategory.DOCUMENT -> "DOC"
                FileCategory.ARCHIVE -> "ZIP"
                FileCategory.OTHER -> "FILE"
            }
    }
}
