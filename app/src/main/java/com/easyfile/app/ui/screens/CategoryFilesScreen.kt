package com.easyfile.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfile.app.model.FileCategory
import com.easyfile.app.model.FileItem
import com.easyfile.app.theme.*
import com.easyfile.app.ui.components.FileCard

@Composable
fun CategoryFilesScreen(
    category: FileCategory,
    files: List<FileItem>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onShareFile: (FileItem) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Top Header Bar
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
                    // Big Senior-friendly Back Button
                    Button(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateBackground,
                            contentColor = NavyDark
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("⬅ Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${category.iconEmoji} ${category.title}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Large Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("🔍 Search files...", fontSize = 16.sp, color = TextSecondaryDark)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
                                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        }

        // File List or Empty State
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DownloadBlue)
            }
        } else if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📂", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No files found here" else "No matching files",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Files saved in this category will appear here.",
                        fontSize = 15.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFiles, key = { it.path }) { item ->
                    FileCard(
                        item = item,
                        onOpen = { onOpenFile(item) },
                        onShare = { onShareFile(item) },
                        onDelete = { onDeleteFile(item) }
                    )
                }
            }
        }
    }
}
