package com.easyfile.app.model

import androidx.compose.ui.graphics.Color
import com.easyfile.app.theme.*

enum class FileCategory(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val accentColor: Color
) {
    PHOTOS(
        title = "Photos & Pictures",
        description = "Camera, Screenshots & Images",
        iconEmoji = "📸",
        accentColor = PhotoOrange
    ),
    WHATSAPP(
        title = "WhatsApp Files",
        description = "Voice notes, Photos & Media",
        iconEmoji = "💬",
        accentColor = WhatsAppGreen
    ),
    AUDIO(
        title = "Voice & Music",
        description = "Voice records & Songs",
        iconEmoji = "🎵",
        accentColor = AudioPurple
    ),
    DOWNLOADS(
        title = "Downloads",
        description = "Saved files, Tickets & Invoices",
        iconEmoji = "📥",
        accentColor = DownloadBlue
    ),
    DOCUMENTS(
        title = "Documents",
        description = "PDFs, Word & Text files",
        iconEmoji = "📄",
        accentColor = DocumentTeal
    ),
    TRASH(
        title = "Recycle Bin",
        description = "Safe place for deleted files",
        iconEmoji = "🗑️",
        accentColor = TrashRed
    )
}
