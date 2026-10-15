package com.easyfile.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfThumbnailHelper {

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    suspend fun getPdfThumbnail(context: Context, pdfFile: File, width: Int = 200, height: Int = 260): Bitmap? =
        withContext(Dispatchers.IO) {
            val cacheKey = "${pdfFile.absolutePath}_${pdfFile.lastModified()}"
            memoryCache.get(cacheKey)?.let { return@withContext it }

            if (!pdfFile.exists() || !pdfFile.canRead()) return@withContext null

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null

            try {
                pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    memoryCache.put(cacheKey, bitmap)
                    bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                try { page?.close() } catch (ignored: Exception) {}
                try { renderer?.close() } catch (ignored: Exception) {}
                try { pfd?.close() } catch (ignored: Exception) {}
            }
        }
}
