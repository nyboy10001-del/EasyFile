package com.easyfile.app.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.easyfile.app.model.FileCategory
import com.easyfile.app.model.FileItem
import com.easyfile.app.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository(private val context: Context) {

    val trashManager = TrashManager(context)

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val stat = StatFs(root.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        StorageStats(totalBytes = totalBytes, freeBytes = freeBytes)
    }

    suspend fun getFilesForCategory(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val results = mutableListOf<FileItem>()

        when (category) {
            FileCategory.PHOTOS -> {
                val folders = listOf(
                    File(root, Environment.DIRECTORY_DCIM),
                    File(root, Environment.DIRECTORY_PICTURES),
                    File(root, "Pictures/Screenshots")
                )
                for (folder in folders) {
                    scanFilesRecursively(folder, maxDepth = 4) { file ->
                        isImageFile(file)
                    }.forEach { results.add(toFileItem(it)) }
                }
            }

            FileCategory.WHATSAPP -> {
                val whatsappFolders = listOf(
                    File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
                    File(root, "WhatsApp/Media")
                )
                for (folder in whatsappFolders) {
                    scanFilesRecursively(folder, maxDepth = 4) { file ->
                        !file.name.startsWith(".")
                    }.forEach { results.add(toFileItem(it)) }
                }
            }

            FileCategory.AUDIO -> {
                val audioFolders = listOf(
                    File(root, Environment.DIRECTORY_MUSIC),
                    File(root, "Recordings"),
                    File(root, "Voice Recorder")
                )
                for (folder in audioFolders) {
                    scanFilesRecursively(folder, maxDepth = 4) { file ->
                        isAudioFile(file)
                    }.forEach { results.add(toFileItem(it)) }
                }
            }

            FileCategory.DOWNLOADS -> {
                val downloads = File(root, Environment.DIRECTORY_DOWNLOADS)
                scanFilesRecursively(downloads, maxDepth = 3) { file ->
                    !file.name.startsWith(".")
                }.forEach { results.add(toFileItem(it)) }
            }

            FileCategory.DOCUMENTS -> {
                val documentFolders = listOf(
                    File(root, Environment.DIRECTORY_DOCUMENTS),
                    File(root, Environment.DIRECTORY_DOWNLOADS)
                )
                for (folder in documentFolders) {
                    scanFilesRecursively(folder, maxDepth = 4) { file ->
                        isDocumentFile(file)
                    }.forEach { results.add(toFileItem(it)) }
                }
            }

            FileCategory.TRASH -> {
                // Handled directly via TrashManager
            }
        }

        // Sort by last modified date (newest first)
        results.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }

    suspend fun getRecentFiles(limit: Int = 10): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val candidateFolders = listOf(
            File(root, Environment.DIRECTORY_DOWNLOADS),
            File(root, Environment.DIRECTORY_DCIM + "/Camera"),
            File(root, "Pictures/Screenshots"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")
        )

        val allFiles = mutableListOf<FileItem>()
        for (folder in candidateFolders) {
            scanFilesRecursively(folder, maxDepth = 2) { file ->
                !file.name.startsWith(".")
            }.forEach { allFiles.add(toFileItem(it)) }
        }

        allFiles.distinctBy { it.path }
            .sortedByDescending { it.lastModified }
            .take(limit)
    }

    suspend fun getCategoryCount(category: FileCategory): Int = withContext(Dispatchers.IO) {
        if (category == FileCategory.TRASH) {
            trashManager.getTrashItems().size
        } else {
            getFilesForCategory(category).size
        }
    }

    private fun scanFilesRecursively(
        dir: File,
        currentDepth: Int = 0,
        maxDepth: Int = 3,
        filter: (File) -> Boolean
    ): List<File> {
        if (!dir.exists() || !dir.isDirectory || currentDepth > maxDepth) return emptyList()
        val found = mutableListOf<File>()
        val children = dir.listFiles() ?: return emptyList()

        for (child in children) {
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                found.addAll(scanFilesRecursively(child, currentDepth + 1, maxDepth, filter))
            } else if (child.isFile && filter(child)) {
                found.add(child)
            }
        }
        return found
    }

    private fun toFileItem(file: File): FileItem {
        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return FileItem(
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory,
            mimeType = mime
        )
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
    }

    private fun isAudioFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp3", "m4a", "wav", "aac", "ogg", "opus", "amr")
    }

    private fun isDocumentFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf")
    }
}
