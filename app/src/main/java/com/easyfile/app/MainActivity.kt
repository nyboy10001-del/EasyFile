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
import com.easyfile.app.ui.screens.HomeScreen
import com.easyfile.app.ui.screens.PermissionScreen
import com.easyfile.app.ui.screens.TrashScreen
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch

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

        // Initialize Google Mobile Ads SDK
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        checkPermissions()

        setContent {
            EasyFileTheme {
                var currentCategory by remember { mutableStateOf<FileCategory?>(null) }
                var storageStats by remember { mutableStateOf<StorageStats?>(null) }
                var categoryCounts by remember { mutableStateOf<Map<FileCategory, Int>>(emptyMap()) }
                var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var categoryFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
                var trashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }
                var isAdFree by remember { mutableStateOf(false) }

                var fileToTrash by remember { mutableStateOf<FileItem?>(null) }

                fun refreshData() {
                    if (!hasStoragePermission) return
                    lifecycleScope.launch {
                        storageStats = repository.getStorageStats()
                        val counts = mutableMapOf<FileCategory, Int>()
                        for (cat in FileCategory.values()) {
                            counts[cat] = repository.getCategoryCount(cat)
                        }
                        categoryCounts = counts
                        recentFiles = repository.getRecentFiles()

                        currentCategory?.let { cat ->
                            if (cat == FileCategory.TRASH) {
                                trashItems = repository.trashManager.getTrashItems()
                            } else {
                                categoryFiles = repository.getFilesForCategory(cat)
                            }
                        }
                    }
                }

                LaunchedEffect(hasStoragePermission, currentCategory) {
                    if (hasStoragePermission) {
                        isLoading = true
                        refreshData()
                        isLoading = false
                    }
                }

                // Delete Confirmation Dialog
                fileToTrash?.let { file ->
                    ConfirmDialog(
                        title = "Move to Recycle Bin?",
                        message = "Are you sure you want to delete '${file.name}'?\n\nDon't worry, it will stay in your Recycle Bin for 30 days and can be recovered anytime.",
                        confirmButtonText = "Move to Trash",
                        onConfirm = {
                            val target = file
                            fileToTrash = null
                            lifecycleScope.launch {
                                repository.trashManager.moveToTrash(target.file)
                                refreshData()
                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = { fileToTrash = null },
                        isDestructive = true
                    )
                }

                Scaffold(
                    bottomBar = {
                        if (hasStoragePermission) {
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
                            when (currentCategory) {
                                null -> {
                                    HomeScreen(
                                        storageStats = storageStats,
                                        categoryCounts = categoryCounts,
                                        recentFiles = recentFiles,
                                        isAdFree = isAdFree,
                                        onCategoryClick = { cat ->
                                            currentCategory = cat
                                        },
                                        onOpenFile = { openFile(it) },
                                        onShareFile = { shareFile(it) },
                                        onDeleteFile = { fileToTrash = it },
                                        onRemoveAdsClick = {
                                            isAdFree = true
                                            Toast.makeText(this@MainActivity, "Ads removed! Thank you for your support.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                                FileCategory.TRASH -> {
                                    TrashScreen(
                                        trashItems = trashItems,
                                        isLoading = isLoading,
                                        onBackClick = { currentCategory = null },
                                        onRestoreItem = { item ->
                                            lifecycleScope.launch {
                                                val restored = repository.trashManager.restoreFile(item)
                                                if (restored) {
                                                    Toast.makeText(this@MainActivity, "File restored!", Toast.LENGTH_SHORT).show()
                                                }
                                                refreshData()
                                            }
                                        },
                                        onDeleteForeverItem = { item ->
                                            lifecycleScope.launch {
                                                repository.trashManager.deletePermanently(item)
                                                refreshData()
                                                Toast.makeText(this@MainActivity, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onEmptyTrash = {
                                            lifecycleScope.launch {
                                                repository.trashManager.emptyTrash()
                                                refreshData()
                                                Toast.makeText(this@MainActivity, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    CategoryFilesScreen(
                                        category = currentCategory!!,
                                        files = categoryFiles,
                                        isLoading = isLoading,
                                        onBackClick = { currentCategory = null },
                                        onOpenFile = { openFile(it) },
                                        onShareFile = { shareFile(it) },
                                        onDeleteFile = { fileToTrash = it }
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

    private fun openFile(item: FileItem) {
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
