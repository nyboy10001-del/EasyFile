package com.easyfile.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyfile.app.model.FileCategory
import com.easyfile.app.theme.*

@Composable
fun CategoryCard(
    category: FileCategory,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SlateSurface)
            .border(2.dp, category.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Emoji / Icon
                Text(
                    text = category.iconEmoji,
                    fontSize = 36.sp
                )

                // Count badge
                if (count != null && count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(category.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$count",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = category.accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = category.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    lineHeight = 22.sp
                )
                Text(
                    text = category.description,
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp,
                    maxLines = 1
                )
            }
        }
    }
}
