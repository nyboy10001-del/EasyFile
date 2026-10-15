package com.easyfile.app.data

import android.content.Context
import android.os.Environment
import com.easyfile.app.model.TrashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TrashManager(private val context: Context) {

    private val trashDir: File
        get() {
            val root = Environment.getExternalStorageDirectory()
            val dir = File(root, ".easyfile_trash")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val metadataFile: File
        get() = File(trashDir, "trash_metadata.tsv")

    suspend fun moveToTrash(sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext false
        try {
            val timestamp = System.currentTimeMillis()
            val sanitizedName = sourceFile.name.replace("\t", "_").replace("\n", "_")
            val targetFile = File(trashDir, "${timestamp}_${sanitizedName}")

            val moved = sourceFile.renameTo(targetFile) || copyAndDelete(sourceFile, targetFile)
            if (moved) {
                // Append to metadata: originalPath\ttrashPath\ttrashedAt\tname\tsizeBytes
                val line = "${sourceFile.absolutePath}\t${targetFile.absolutePath}\t$timestamp\t$sanitizedName\t${targetFile.length()}\n"
                FileOutputStream(metadataFile, true).use {
                    it.write(line.toByteArray(Charsets.UTF_8))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFile(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val trashFile = File(item.trashPath)
        val originalFile = File(item.originalPath)

        if (!trashFile.exists()) {
            removeMetadataEntry(item)
            return@withContext false
        }

        try {
            originalFile.parentFile?.mkdirs()
            val restored = trashFile.renameTo(originalFile) || copyAndDelete(trashFile, originalFile)
            if (restored) {
                removeMetadataEntry(item)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePermanently(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val trashFile = File(item.trashPath)
        if (trashFile.exists()) {
            trashFile.delete()
        }
        removeMetadataEntry(item)
        true
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        val items = getTrashItems()
        for (item in items) {
            File(item.trashPath).delete()
        }
        if (metadataFile.exists()) {
            metadataFile.delete()
        }
        true
    }

    suspend fun getTrashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        if (!metadataFile.exists()) return@withContext emptyList()
        val items = mutableListOf<TrashItem>()
        try {
            metadataFile.readLines(Charsets.UTF_8).forEach { line ->
                val parts = line.split("\t")
                if (parts.size >= 5) {
                    val originalPath = parts[0]
                    val trashPath = parts[1]
                    val trashedAt = parts[2].toLongOrNull() ?: 0L
                    val name = parts[3]
                    val sizeBytes = parts[4].toLongOrNull() ?: 0L

                    // Check if file still exists in trash
                    if (File(trashPath).exists()) {
                        items.add(TrashItem(originalPath, trashPath, trashedAt, name, sizeBytes))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items.reversed() // Most recently deleted on top
    }

    private fun removeMetadataEntry(item: TrashItem) {
        if (!metadataFile.exists()) return
        try {
            val lines = metadataFile.readLines(Charsets.UTF_8).filter {
                !it.startsWith(item.originalPath + "\t")
            }
            metadataFile.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "", Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyAndDelete(source: File, destination: File): Boolean {
        return try {
            source.copyTo(destination, overwrite = true)
            source.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
