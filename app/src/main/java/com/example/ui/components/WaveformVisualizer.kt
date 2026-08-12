package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isRecording: Boolean,
    amplitude: Int,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val barCount = 32
        val barSpacing = width / barCount
        val normalizedAmp = if (isRecording) {
            (amplitude / 12000f).coerceIn(0.15f, 1.0f)
        } else {
            0.05f
        }

        for (i in 0 until barCount) {
            val x = i * barSpacing + (barSpacing / 2f)
            val sineFactor = sin(phase + i * 0.35f)
            val barHeight = if (isRecording) {
                (height * 0.15f) + (height * 0.7f * normalizedAmp * ((sineFactor + 1f) / 2f))
            } else {
                height * 0.08f
            }

            val topY = (centerY - (barHeight / 2f)).coerceAtLeast(4f)
            val bottomY = (centerY + (barHeight / 2f)).coerceAtMost(height - 4f)

            drawLine(
                color = if (isRecording) barColor else barColor.copy(alpha = 0.3f),
                start = Offset(x, topY),
                end = Offset(x, bottomY),
                strokeWidth = (barSpacing * 0.55f).coerceIn(4f, 16f),
                cap = StrokeCap.Round
            )
        }
    }
}
