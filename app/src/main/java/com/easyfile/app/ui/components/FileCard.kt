package com.easyfile.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easyfile.app.model.FileItem
import com.easyfile.app.theme.*

@Composable
fun FileCard(
    item: FileItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            // Thumbnail or File Type Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.isImage) {
                    AsyncImage(
                        model = item.file,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when {
                        item.isAudio -> "🎵"
                        item.isVideo -> "🎬"
                        item.isPdf -> "📄"
                        else -> "📁"
                    }
                    Text(text = icon, fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = DownloadBlue
                    )
                    Text(
                        text = " • ",
                        fontSize = 14.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = item.formattedDate,
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Action Buttons - Designed for accessibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // OPEN Button (Primary)
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = Color.White
                )
            ) {
                Text("👁️ Open", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // SHARE Button
            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                )
            ) {
                Text("📤 Share", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // DELETE Button (Trash)
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TrashRed
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
            ) {
                Text("🗑️ Trash", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrashRed)
            }
        }
    }
}
