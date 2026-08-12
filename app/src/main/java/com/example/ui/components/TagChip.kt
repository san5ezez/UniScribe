package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TagEntity

fun parseColorHex(colorHex: String, defaultColor: Color = Color(0xFF3F51B5)): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun SubjectTagChip(
    tag: TagEntity,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tagColor = parseColorHex(tag.colorHex)

    if (onClick != null) {
        FilterChip(
            selected = isSelected,
            onClick = onClick,
            label = {
                Text(
                    text = tag.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = tagColor.copy(alpha = 0.2f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier.testTag("tag_chip_${tag.id}")
        )
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = tagColor.copy(alpha = 0.15f),
            modifier = modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tag.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
