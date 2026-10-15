package com.easyfile.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.easyfile.app.data.FileManagerRepository
import com.easyfile.app.model.FileCategory
import com.easyfile.app.model.FileItem
import com.easyfile.app.model.StorageStats
import com.easyfile.app.model.TrashItem
import com.easyfile.app.theme.EasyFileTheme
import com.easyfile.app.ui.components.AdBanner
import com.easyfile.app.ui.components.ConfirmDialog
import com.easyfile.app.ui.screens.CategoryFilesScreen
import com.easyfile.app.ui.screens.FolderExplorerScreen
import com.easyfile.app.ui.screens.HomeScreen
import com.easyfile.app.ui.screens.PermissionScreen
import com.easyfile.app.ui.screens.TrashScreen
import com.easyfile.app.ui.viewers.ImageViewerScreen
import com.easyfile.app.ui.viewers.MediaPlaybackScreen
import com.easyfile.app.ui.viewers.PdfViewerScreen
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class Screen {
    object Home : Screen()
    data class CategoryFiles(val category: FileCategory) : Screen()
    data class FolderExplorer(val folder: File) : Screen()
    data class ViewPdf(val fileItem: FileItem, val previousScreen: Screen) : Screen()
    data class ViewMedia(val fileItem: FileItem, val previousScreen: Screen) : Screen()
    data class ViewImage(val fileItem: FileItem, val previousScreen: Screen) : Screen()
    object Trash : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: FileManagerRepository
    private var hasStoragePermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FileManagerRepository(applicationContext)

        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        checkPermissions()

        setContent {
            EasyFileTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                var storageStats by remember { mutableStateOf<StorageStats?>(null) }
                var categoryCounts by remember { mutableStateOf<Map<FileCategory, Int>>(emptyMap()) }
                var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var categoryFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var folderSubfolders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var folderFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var trashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }
                var isAdFree by remember { mutableStateOf(false) }

                var fileToTrash by remember { mutableStateOf<FileItem?>(null) }

                // Smooth background data loading
                fun loadInitialData() {
                    if (!hasStoragePermission) return
                    lifecycleScope.launch {
                        // Instant phase: Stats & Recents (sub-50ms)
                        storageStats = repository.getStorageStats()
                        recentFiles = repository.getRecentFiles(limit = 15)

                        // Async phase: load counts in background without blocking Home UI
                        withContext(Dispatchers.IO) {
                            val counts = mutableMapOf<FileCategory, Int>()
                            for (cat in FileCategory.values()) {
                                if (cat == FileCategory.TRASH) {
                                    counts[cat] = repository.trashManager.getTrashItems().size
                                } else {
                                    counts[cat] = repository.getFilesForCategory(cat).size
                                }
                            }
                            categoryCounts = counts
                        }
                    }
                }

                fun loadCategory(category: FileCategory) {
                    if (category == FileCategory.TRASH) {
                        currentScreen = Screen.Trash
                        lifecycleScope.launch {
                            isLoading = true
                            trashItems = repository.trashManager.getTrashItems()
                            isLoading = false
                        }
                    } else {
                        currentScreen = Screen.CategoryFiles(category)
                        lifecycleScope.launch {
                            isLoading = true
                            categoryFiles = repository.getFilesForCategory(category)
                            isLoading = false
                        }
                    }
                }

                fun loadFolder(folder: File) {
                    currentScreen = Screen.FolderExplorer(folder)
                    lifecycleScope.launch {
                        isLoading = true
                        val (folders, files) = repository.getDirectoryContents(folder)
                        folderSubfolders = folders
                        folderFiles = files
                        isLoading = false
                    }
                }

                fun handleOpenFile(fileItem: FileItem) {
                    when {
                        fileItem.isPdf -> {
                            currentScreen = Screen.ViewPdf(fileItem, currentScreen)
                        }
                        fileItem.isVideo || fileItem.isAudio -> {
                            currentScreen = Screen.ViewMedia(fileItem, currentScreen)
                        }
                        fileItem.isImage -> {
                            currentScreen = Screen.ViewImage(fileItem, currentScreen)
                        }
                        else -> {
                            // Fallback to system handler for APK, Word, Excel, etc.
                            openFileInSystem(fileItem)
                        }
                    }
                }

                LaunchedEffect(hasStoragePermission) {
                    if (hasStoragePermission) {
                        loadInitialData()
                    }
                }

                // Handle Android Back Key
                BackHandler(enabled = currentScreen !is Screen.Home) {
                    when (val screen = currentScreen) {
                        is Screen.ViewPdf -> currentScreen = screen.previousScreen
                        is Screen.ViewMedia -> currentScreen = screen.previousScreen
                        is Screen.ViewImage -> currentScreen = screen.previousScreen
                        is Screen.CategoryFiles -> currentScreen = Screen.Home
                        is Screen.Trash -> currentScreen = Screen.Home
                        is Screen.FolderExplorer -> {
                            val root = Environment.getExternalStorageDirectory()
                            if (screen.folder.absolutePath == root.absolutePath || screen.folder.parentFile == null) {
                                currentScreen = Screen.Home
                            } else {
                                loadFolder(screen.folder.parentFile!!)
                            }
                        }
                        Screen.Home -> {}
                    }
                }

                // Delete Confirmation Dialog
                fileToTrash?.let { file ->
                    ConfirmDialog(
                        title = "Move to Recycle Bin?",
                        message = "Are you sure you want to delete '${file.name}'?\n\nDon't worry, it stays in your Recycle Bin for 30 days and can be recovered anytime.",
                        confirmButtonText = "Move to Trash",
                        onConfirm = {
                            val target = file
                            fileToTrash = null
                            lifecycleScope.launch {
                                repository.trashManager.moveToTrash(target.file)
                                loadInitialData()
                                when (val screen = currentScreen) {
                                    is Screen.CategoryFiles -> categoryFiles = repository.getFilesForCategory(screen.category)
                                    is Screen.FolderExplorer -> {
                                        val (folders, files) = repository.getDirectoryContents(screen.folder)
                                        folderSubfolders = folders
                                        folderFiles = files
                                    }
                                    is Screen.ViewImage -> currentScreen = screen.previousScreen
                                    else -> {}
                                }
                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = { fileToTrash = null },
                        isDestructive = true
                    )
                }

                Scaffold(
                    bottomBar = {
                        if (hasStoragePermission && currentScreen !is Screen.ViewMedia && currentScreen !is Screen.ViewImage) {
                            AdBanner(isAdFree = isAdFree)
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        if (!hasStoragePermission) {
                            PermissionScreen(
                                onGrantPermissionClick = { requestStoragePermission() }
                            )
                        } else {
                            when (val screen = currentScreen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        storageStats = storageStats,
                                        categoryCounts = categoryCounts,
                                        recentFiles = recentFiles,
                                        isAdFree = isAdFree,
                                        onCategoryClick = { cat -> loadCategory(cat) },
                                        onExploreFoldersClick = {
                                            loadFolder(Environment.getExternalStorageDirectory())
                                        },
                                        onOpenFile = { handleOpenFile(it) },
                                        onShareFile = { shareFile(it) },
                                        onDeleteFile = { fileToTrash = it },
                                        onRemoveAdsClick = {
                                            isAdFree = true
                                            Toast.makeText(this@MainActivity, "Ads removed! Thank you for your support.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }

                                is Screen.CategoryFiles -> {
                                    CategoryFilesScreen(
                                        category = screen.category,
                                        files = categoryFiles,
                                        isLoading = isLoading,
                                        onBackClick = { currentScreen = Screen.Home },
                                        onOpenFile = { handleOpenFile(it) },
                                        onShareFile = { shareFile(it) },
                                        onDeleteFile = { fileToTrash = it }
                                    )
                                }

                                is Screen.FolderExplorer -> {
                                    FolderExplorerScreen(
                                        currentFolder = screen.folder,
                                        folders = folderSubfolders,
                                        files = folderFiles,
                                        isLoading = isLoading,
                                        onNavigateToFolder = { loadFolder(it) },
                                        onNavigateUp = {
                                            val root = Environment.getExternalStorageDirectory()
                                            if (screen.folder.absolutePath == root.absolutePath || screen.folder.parentFile == null) {
                                                currentScreen = Screen.Home
                                            } else {
                                                loadFolder(screen.folder.parentFile!!)
                                            }
                                        },
                                        onBackToHome = { currentScreen = Screen.Home },
                                        onOpenFile = { handleOpenFile(it) },
                                        onShareFile = { shareFile(it) },
                                        onDeleteFile = { fileToTrash = it }
                                    )
                                }

                                is Screen.ViewPdf -> {
                                    PdfViewerScreen(
                                        fileItem = screen.fileItem,
                                        onBackClick = { currentScreen = screen.previousScreen },
                                        onShareClick = { shareFile(screen.fileItem) }
                                    )
                                }

                                is Screen.ViewMedia -> {
                                    MediaPlaybackScreen(
                                        fileItem = screen.fileItem,
                                        onBackClick = { currentScreen = screen.previousScreen },
                                        onShareClick = { shareFile(screen.fileItem) }
                                    )
                                }

                                is Screen.ViewImage -> {
                                    ImageViewerScreen(
                                        fileItem = screen.fileItem,
                                        onBackClick = { currentScreen = screen.previousScreen },
                                        onShareClick = { shareFile(screen.fileItem) },
                                        onDeleteClick = { fileToTrash = screen.fileItem }
                                    )
                                }

                                is Screen.Trash -> {
                                    TrashScreen(
                                        trashItems = trashItems,
                                        isLoading = isLoading,
                                        onBackClick = { currentScreen = Screen.Home },
                                        onRestoreItem = { item ->
                                            lifecycleScope.launch {
                                                val restored = repository.trashManager.restoreFile(item)
                                                if (restored) {
                                                    Toast.makeText(this@MainActivity, "File restored!", Toast.LENGTH_SHORT).show()
                                                }
                                                trashItems = repository.trashManager.getTrashItems()
                                                loadInitialData()
                                            }
                                        },
                                        onDeleteForeverItem = { item ->
                                            lifecycleScope.launch {
                                                repository.trashManager.deletePermanently(item)
                                                trashItems = repository.trashManager.getTrashItems()
                                                loadInitialData()
                                                Toast.makeText(this@MainActivity, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onEmptyTrash = {
                                            lifecycleScope.launch {
                                                repository.trashManager.emptyTrash()
                                                trashItems = repository.trashManager.getTrashItems()
                                                loadInitialData()
                                                Toast.makeText(this@MainActivity, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            readGranted
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun openFileInSystem(item: FileItem) {
        try {
            val file = item.file
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(item: FileItem) {
        try {
            val file = item.file
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share ${item.name}"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
