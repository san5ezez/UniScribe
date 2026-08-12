package com.example.ui.tabs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LectureViewModel
import com.example.ui.components.SubjectTagChip
import com.example.ui.components.WaveformVisualizer
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingTab(
    viewModel: LectureViewModel,
    modifier: Modifier = Modifier
) {
    val isRecording by viewModel.recorderManager.isRecording.collectAsState()
    val durationSeconds by viewModel.recorderManager.recordingDurationSeconds.collectAsState()
    val amplitude by viewModel.recorderManager.currentAmplitude.collectAsState()
    val recordingTitle by viewModel.recordingTitle.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagIds by viewModel.selectedRecordingTagIds.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lecture title input
            OutlinedTextField(
                value = recordingTitle,
                onValueChange = { viewModel.setRecordingTitle(it) },
                label = { Text("Название занятия / лекции") },
                placeholder = { Text("Напр. Высшая математика — Лекция №4") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                enabled = !isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recording_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Select Subject Tags
            Text(
                text = "Выберите предмет (теги):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allTags.forEach { tag ->
                    val isSelected = selectedTagIds.contains(tag.id)
                    SubjectTagChip(
                        tag = tag,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleRecordingTag(tag.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Offline / Online Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Status",
                        tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isOnline) "Офлайн-запись + расшифровка на лету" else "Офлайн-режим (запись в память)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isOnline) "Аудио сохраняется локально, расшифровка отправляется в Gemini API"
                            else "Запись надежно сохраняется на устройстве и расшифруется автоматически при появлении интернета",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Area: Recording Visualizer & Timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live Waveform Visualizer
            WaveformVisualizer(
                isRecording = isRecording,
                amplitude = amplitude,
                barColor = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration Display
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            val timeString = if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            Text(
                text = timeString,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isRecording) "ИДЕТ ЗАПИСЬ РЕЧИ ПРЕПОДАВАТЕЛЯ..." else "Нажмите кнопку ниже для старта записи",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Prominent Single-Touch Start/Stop Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFFFCDD2).copy(alpha = 0.5f))
                    )
                }

                Surface(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecordingAndSave()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = CircleShape,
                    color = if (isRecording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(110.dp)
                        .testTag("record_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Стоп" else "Старт записи",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRecording) "НАЖМИТЕ, ЧТОБЫ СОХРАНИТЬ ЛЕКЦИЮ" else "ОДНО КАСАНИЕ ДЛЯ СТАРТА",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }
    }
}
