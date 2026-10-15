package com.easyfile.app.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean = false,
    val mimeType: String? = null
) {
    val file: File get() = File(path)

    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true ||
                listOf("jpg", "jpeg", "png", "webp", "gif", "bmp").any { name.lowercase().endsWith(".$it") }

    val isAudio: Boolean
        get() = mimeType?.startsWith("audio/") == true ||
                listOf("mp3", "m4a", "wav", "aac", "ogg", "opus").any { name.lowercase().endsWith(".$it") }

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true ||
                listOf("mp4", "mkv", "avi", "mov", "3gp").any { name.lowercase().endsWith(".$it") }

    val isPdf: Boolean
        get() = name.lowercase().endsWith(".pdf") || mimeType == "application/pdf"
}
