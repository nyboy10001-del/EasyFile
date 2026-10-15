package com.easyfile.app.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.easyfile.app.model.FileCategory
import com.easyfile.app.model.FileItem
import com.easyfile.app.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository(private val context: Context) {

    val trashManager = TrashManager(context)
    private val contentResolver: ContentResolver get() = context.contentResolver

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
        when (category) {
            FileCategory.PHOTOS -> queryImages()
            FileCategory.WHATSAPP -> queryWhatsAppFiles()
            FileCategory.AUDIO -> queryAudio()
            FileCategory.DOWNLOADS -> queryDownloads()
            FileCategory.DOCUMENTS -> queryDocuments()
            FileCategory.TRASH -> emptyList() // Handled via trashManager
        }
    }

    suspend fun getRecentFiles(limit: Int = 15): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $limit"
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0 AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE '%/.%'"

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    results.add(
                        FileItem(
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                            isDirectory = false,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    /**
     * Folder-by-folder browser for Main Storage
     */
    suspend fun getDirectoryContents(folder: File): Pair<List<FileItem>, List<FileItem>> = withContext(Dispatchers.IO) {
        if (!folder.exists() || !folder.isDirectory || !folder.canRead()) {
            return@withContext Pair(emptyList(), emptyList())
        }

        val foldersList = mutableListOf<FileItem>()
        val filesList = mutableListOf<FileItem>()

        val children = folder.listFiles() ?: return@withContext Pair(emptyList(), emptyList())

        for (child in children) {
            if (child.name.startsWith(".")) continue // Skip hidden files
            if (child.isDirectory) {
                val count = child.list()?.count { !it.startsWith(".") } ?: 0
                foldersList.add(
                    FileItem(
                        name = child.name,
                        path = child.absolutePath,
                        sizeBytes = 0L,
                        lastModified = child.lastModified(),
                        isDirectory = true,
                        itemCount = count
                    )
                )
            } else if (child.isFile) {
                filesList.add(toFileItem(child))
            }
        }

        // Sort folders alphabetically, files by name or newest
        val sortedFolders = foldersList.sortedBy { it.name.lowercase() }
        val sortedFiles = filesList.sortedByDescending { it.lastModified }

        Pair(sortedFolders, sortedFiles)
    }

    private fun queryImages(): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    results.add(
                        FileItem(
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                            isDirectory = false,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun queryAudio(): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    results.add(
                        FileItem(
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                            isDirectory = false,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun queryDownloads(): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val root = Environment.getExternalStorageDirectory()
        val downloadsDir = File(root, Environment.DIRECTORY_DOWNLOADS)

        if (downloadsDir.exists() && downloadsDir.isDirectory) {
            downloadsDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.startsWith(".")) {
                    results.add(toFileItem(file))
                }
            }
        }

        // Also query MediaStore Downloads if available on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.Downloads.DATA,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_MODIFIED,
                    MediaStore.Downloads.MIME_TYPE
                )
                contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATA)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataCol) ?: continue
                        val file = File(path)
                        if (!file.exists() || file.isDirectory) continue

                        val name = cursor.getString(nameCol) ?: file.name
                        val size = cursor.getLong(sizeCol)
                        val dateModified = cursor.getLong(dateCol) * 1000L
                        val mime = cursor.getString(mimeCol)

                        results.add(
                            FileItem(
                                name = name,
                                path = path,
                                sizeBytes = size,
                                lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                                isDirectory = false,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return results.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }

    private fun queryDocuments(): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val docExtensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt", "rtf")

        // Query MediaStore Files for documents
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        docExtensions.forEachIndexed { index, ext ->
            if (index > 0) selection.append(" OR ")
            selection.append("${MediaStore.Files.FileColumns.DATA} LIKE ?")
            selectionArgs.add("%.$ext")
        }

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                "(${selection}) AND ${MediaStore.Files.FileColumns.SIZE} > 0",
                selectionArgs.toTypedArray(),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    results.add(
                        FileItem(
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                            isDirectory = false,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also check standard Documents folder directly
        val root = Environment.getExternalStorageDirectory()
        val docDir = File(root, Environment.DIRECTORY_DOCUMENTS)
        if (docDir.exists() && docDir.isDirectory) {
            docDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.startsWith(".")) {
                    results.add(toFileItem(file))
                }
            }
        }

        return results.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }

    /**
     * Dedicated WhatsApp Scanner: Supports both modern Android 11+ and legacy folder structures
     */
    private fun queryWhatsAppFiles(): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val root = Environment.getExternalStorageDirectory()

        val candidateDirs = listOf(
            // Modern Android 11+ standard location
            File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
            // WhatsApp Business modern location
            File(root, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
            // Legacy location (Android 10 and below)
            File(root, "WhatsApp/Media")
        )

        for (baseMediaDir in candidateDirs) {
            if (!baseMediaDir.exists() || !baseMediaDir.isDirectory) continue

            val subDirs = baseMediaDir.listFiles() ?: continue
            for (subDir in subDirs) {
                if (!subDir.isDirectory) continue
                // Scan images, video, audio, voice notes, documents
                scanFilesFlat(subDir, maxFiles = 100) { file ->
                    !file.name.startsWith(".") && file.length() > 0
                }.forEach { results.add(toFileItem(it)) }
            }
        }

        // Also query MediaStore with WhatsApp filter for instant results
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "(${MediaStore.Files.FileColumns.DATA} LIKE '%com.whatsapp%' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%/WhatsApp/%') AND ${MediaStore.Files.FileColumns.SIZE} > 0"

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 200"
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    results.add(
                        FileItem(
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModified = if (dateModified > 0) dateModified else file.lastModified(),
                            isDirectory = false,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }

    private fun scanFilesFlat(dir: File, maxFiles: Int = 100, filter: (File) -> Boolean): List<File> {
        val list = mutableListOf<File>()
        val files = dir.listFiles() ?: return emptyList()
        for (f in files) {
            if (f.isFile && filter(f)) {
                list.add(f)
                if (list.size >= maxFiles) break
            } else if (f.isDirectory && !f.name.equals("Private", ignoreCase = true)) {
                // Scan 1 subfolder deep (e.g. Sent or Voice Notes dates)
                f.listFiles()?.forEach { sub ->
                    if (sub.isFile && filter(sub)) {
                        list.add(sub)
                    }
                }
            }
        }
        return list
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
            itemCount = if (file.isDirectory) file.list()?.size else null,
            mimeType = mime
        )
    }
}
