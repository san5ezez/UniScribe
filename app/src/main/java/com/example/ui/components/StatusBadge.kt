package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TranscriptionStatus

@Composable
fun StatusBadge(
    status: TranscriptionStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon, label) = when (status) {
        TranscriptionStatus.TRANSCRIBED -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle,
            "Расшифровано"
        )
        TranscriptionStatus.PENDING_INTERNET -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.WifiOff,
            "Ожидает интернета"
        )
        TranscriptionStatus.TRANSCRIBING -> Quadruple(
            Color(0xFFE8EAF6),
            Color(0xFF303F9F),
            Icons.Default.Sync,
            "Расшифровка..."
        )
        TranscriptionStatus.ERROR -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.Error,
            "Ошибка расшифровки"
        )
    }

    val rotation = if (status == TranscriptionStatus.TRANSCRIBING) {
        val infiniteTransition = rememberInfiniteTransition(label = "spin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
        angle
    } else 0f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier
                .size(14.dp)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
