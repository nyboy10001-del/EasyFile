package com.easyfile.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.easyfile.app.model.TrashItem
import com.easyfile.app.theme.*
import com.easyfile.app.ui.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashScreen(
    trashItems: List<TrashItem>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onRestoreItem: (TrashItem) -> Unit,
    onDeleteForeverItem: (TrashItem) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeleteForever by remember { mutableStateOf<TrashItem?>(null) }

    if (showEmptyTrashConfirm) {
        ConfirmDialog(
            title = "Empty Recycle Bin?",
            message = "All ${trashItems.size} items in the Recycle Bin will be permanently deleted. This cannot be undone.",
            confirmButtonText = "Empty Bin",
            onConfirm = {
                showEmptyTrashConfirm = false
                onEmptyTrash()
            },
            onDismiss = { showEmptyTrashConfirm = false },
            isDestructive = true
        )
    }

    itemToDeleteForever?.let { item ->
        ConfirmDialog(
            title = "Delete Permanently?",
            message = "Are you sure you want to permanently delete '${item.name}'? You will not be able to recover it.",
            confirmButtonText = "Delete Forever",
            onConfirm = {
                itemToDeleteForever = null
                onDeleteForeverItem(item)
            },
            onDismiss = { itemToDeleteForever = null },
            isDestructive = true
        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "🗑️ Recycle Bin",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                if (trashItems.isNotEmpty()) {
                    Button(
                        onClick = { showEmptyTrashConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrashRed.copy(alpha = 0.15f),
                            contentColor = TrashRed
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Empty Bin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Safety Notice Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WhatsAppGreen.copy(alpha = 0.1f))
                .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "🛡️ Safe Protection: Deleted files are kept safely here. You can click 'Put Back' anytime to restore them.",
                fontSize = 15.sp,
                color = TextPrimaryDark,
                lineHeight = 20.sp
            )
        }

        // List or Empty
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DownloadBlue)
            }
        } else if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✨", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recycle Bin is Empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You haven't deleted any files recently.",
                        fontSize = 15.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trashItems, key = { it.trashPath }) { item ->
                    TrashCard(
                        item = item,
                        onRestore = { onRestoreItem(item) },
                        onDeleteForever = { itemToDeleteForever = item }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashCard(
    item: TrashItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val dateStr = remember(item.trashedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(item.trashedAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateSurface)
            .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📄", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
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
                    text = "Deleted: $dateStr",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Put Back (Restore) Button - Big & Prominent
            Button(
                onClick = onRestore,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                )
            ) {
                Text("↩️ Put Back", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Delete Forever Button
            OutlinedButton(
                onClick = onDeleteForever,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrashRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrashRed)
            }
        }
    }
}
