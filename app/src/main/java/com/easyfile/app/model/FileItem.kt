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
    val itemCount: Int? = null,
    val mimeType: String? = null
) {
    val file: File get() = File(path)

    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (itemCount != null) "$itemCount items" else "Folder"
            }
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

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true ||
                extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg")

    val isAudio: Boolean
        get() = mimeType?.startsWith("audio/") == true ||
                extension in listOf("mp3", "m4a", "wav", "aac", "ogg", "opus", "amr", "flac")

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true ||
                extension in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "ts")

    val isPdf: Boolean
        get() = extension == "pdf" || mimeType == "application/pdf"

    val isExcel: Boolean
        get() = extension in listOf("xls", "xlsx", "csv") ||
                mimeType in listOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

    val isWord: Boolean
        get() = extension in listOf("doc", "docx") ||
                mimeType in listOf("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

    val isPowerPoint: Boolean
        get() = extension in listOf("ppt", "pptx")

    val isZip: Boolean
        get() = extension in listOf("zip", "rar", "7z", "tar", "gz")

    val isApk: Boolean
        get() = extension == "apk"
}
