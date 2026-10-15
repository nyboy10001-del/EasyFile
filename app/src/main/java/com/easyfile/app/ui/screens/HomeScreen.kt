package com.easyfile.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfile.app.model.FileCategory
import com.easyfile.app.model.FileItem
import com.easyfile.app.model.StorageStats
import com.easyfile.app.theme.*
import com.easyfile.app.ui.components.CategoryCard
import com.easyfile.app.ui.components.FileCard

@Composable
fun HomeScreen(
    storageStats: StorageStats?,
    categoryCounts: Map<FileCategory, Int>,
    recentFiles: List<FileItem>,
    isAdFree: Boolean,
    onCategoryClick: (FileCategory) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onShareFile: (FileItem) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onRemoveAdsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title & Remove Ads Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📁 EasyFile",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }

                if (!isAdFree) {
                    Button(
                        onClick = onRemoveAdsClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DownloadBlue,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("⭐ Remove Ads", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Storage Overview Card
        if (storageStats != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateSurface)
                        .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💾 Phone Memory",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${storageStats.readableFree} Free",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DownloadBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Clear visual progress bar
                        LinearProgressIndicator(
                            progress = { storageStats.percentageUsed },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = StorageBarFill,
                            trackColor = StorageBarBackground,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Using ${storageStats.readableUsed} of ${storageStats.readableTotal}",
                            fontSize = 14.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }

        // 6 Big Category Cards
        item {
            Text(
                text = "Categories",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Photos & WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        category = FileCategory.PHOTOS,
                        count = categoryCounts[FileCategory.PHOTOS],
                        onClick = { onCategoryClick(FileCategory.PHOTOS) },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryCard(
                        category = FileCategory.WHATSAPP,
                        count = categoryCounts[FileCategory.WHATSAPP],
                        onClick = { onCategoryClick(FileCategory.WHATSAPP) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Audio & Downloads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        category = FileCategory.AUDIO,
                        count = categoryCounts[FileCategory.AUDIO],
                        onClick = { onCategoryClick(FileCategory.AUDIO) },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryCard(
                        category = FileCategory.DOWNLOADS,
                        count = categoryCounts[FileCategory.DOWNLOADS],
                        onClick = { onCategoryClick(FileCategory.DOWNLOADS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Documents & Recycle Bin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        category = FileCategory.DOCUMENTS,
                        count = categoryCounts[FileCategory.DOCUMENTS],
                        onClick = { onCategoryClick(FileCategory.DOCUMENTS) },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryCard(
                        category = FileCategory.TRASH,
                        count = categoryCounts[FileCategory.TRASH],
                        onClick = { onCategoryClick(FileCategory.TRASH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Files Section
        if (recentFiles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recently Added Files",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            items(recentFiles, key = { it.path }) { fileItem ->
                FileCard(
                    item = fileItem,
                    onOpen = { onOpenFile(fileItem) },
                    onShare = { onShareFile(fileItem) },
                    onDelete = { onDeleteFile(fileItem) }
                )
            }
        }
    }
}
