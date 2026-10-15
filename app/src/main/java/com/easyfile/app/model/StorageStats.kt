package com.easyfile.app.model

import java.util.Locale

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val percentageUsed: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f

    val readableFree: String get() = formatBytes(freeBytes)
    val readableTotal: String get() = formatBytes(totalBytes)
    val readableUsed: String get() = formatBytes(usedBytes)

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f GB", gb)
    }
}

data class TrashItem(
    val originalPath: String,
    val trashPath: String,
    val trashedAt: Long,
    val name: String,
    val sizeBytes: Long
)
