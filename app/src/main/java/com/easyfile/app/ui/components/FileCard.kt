package com.easyfile.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easyfile.app.data.PdfThumbnailHelper
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
    val context = LocalContext.current
    var pdfThumbnail by remember(item.path) { mutableStateOf<Bitmap?>(null) }

    if (item.isPdf) {
        LaunchedEffect(item.path) {
            pdfThumbnail = PdfThumbnailHelper.getPdfThumbnail(context, item.file)
        }
    }

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
            // Large Thumbnail Preview (80dp x 80dp)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isImage -> {
                        AsyncImage(
                            model = item.file,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    item.isVideo -> {
                        AsyncImage(
                            model = item.file,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Play icon overlay
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", fontSize = 16.sp, color = Color.White)
                        }
                    }
                    item.isPdf -> {
                        if (pdfThumbnail != null) {
                            Image(
                                bitmap = pdfThumbnail!!.asImageBitmap(),
                                contentDescription = "PDF Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 32.sp)
                                Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrashRed)
                            }
                        }
                    }
                    item.isExcel -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 32.sp)
                            Text("EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                        }
                    }
                    item.isWord -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 32.sp)
                            Text("WORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DownloadBlue)
                        }
                    }
                    item.isAudio -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎵", fontSize = 32.sp)
                            Text("AUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AudioPurple)
                        }
                    }
                    item.isDirectory -> {
                        Text("📁", fontSize = 36.sp)
                    }
                    else -> {
                        Text("📄", fontSize = 34.sp)
                    }
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large, Non-Clipping Action Buttons
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
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text("👁️ Open", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }

            // SHARE Button
            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text("📤 Share", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }

            // DELETE Button (Trash)
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TrashRed
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text("🗑️ Trash", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrashRed, maxLines = 1)
            }
        }
    }
}
