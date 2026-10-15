package com.easyfile.app.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfile.app.model.FileItem
import com.easyfile.app.theme.*
import com.easyfile.app.ui.components.FileCard
import java.io.File

@Composable
fun FolderExplorerScreen(
    currentFolder: File,
    folders: List<FileItem>,
    files: List<FileItem>,
    isLoading: Boolean,
    onNavigateToFolder: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onBackToHome: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onShareFile: (FileItem) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isRoot = remember(currentFolder) {
        currentFolder.absolutePath == Environment.getExternalStorageDirectory().absolutePath
    }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val displayPath = remember(currentFolder) {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val rel = currentFolder.absolutePath.removePrefix(rootPath).trim('/')
        if (rel.isEmpty()) "Internal Storage" else "Internal Storage > ${rel.replace("/", " > ")}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Top Header
        Surface(
            color = SlateSurface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBackToHome,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateBackground,
                            contentColor = NavyDark
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("🏠 Home", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (isRoot) "📁 Internal Storage" else "📁 ${currentFolder.name}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Breadcrumb path display
                Text(
                    text = displayPath,
                    fontSize = 13.sp,
                    color = DownloadBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("🔍 Search in this folder...", fontSize = 15.sp, color = TextSecondaryDark)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateBackground,
                        unfocusedContainerColor = SlateBackground,
                        focusedBorderColor = DownloadBlue,
                        unfocusedBorderColor = CardBorder
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        }

        // Folder & File Listing
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DownloadBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "Go Up" Card (Visible when inside a subfolder)
                if (!isRoot) {
                    item {
                        val parentName = currentFolder.parentFile?.name ?: "Parent Folder"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFEDE9FE))
                                .border(1.5.dp, AudioPurple.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateUp)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⬆️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Go Up to $parentName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AudioPurple
                                    )
                                    Text(
                                        text = "Return to previous folder",
                                        fontSize = 13.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Subfolders Section
                if (filteredFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Folders (${filteredFolders.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredFolders, key = { it.path }) { folderItem ->
                        FolderTile(
                            item = folderItem,
                            onClick = { onNavigateToFolder(folderItem.file) }
                        )
                    }
                }

                // Files Section
                if (filteredFiles.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Files (${filteredFiles.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredFiles, key = { it.path }) { fileItem ->
                        FileCard(
                            item = fileItem,
                            onOpen = { onOpenFile(fileItem) },
                            onShare = { onShareFile(fileItem) },
                            onDelete = { onDeleteFile(fileItem) }
                        )
                    }
                }

                // Empty folder
                if (filteredFolders.isEmpty() && filteredFiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📂", fontSize = 56.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "This folder is empty" else "No matching items",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTile(
    item: FileItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateSurface)
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📁", fontSize = 34.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.formattedSize, // e.g. "14 items"
                    fontSize = 13.sp,
                    color = DownloadBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("➔", fontSize = 20.sp, color = DownloadBlue)
        }
    }
}
