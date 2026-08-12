package com.example.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.LectureWithTags
import com.example.data.db.TranscriptionStatus
import com.example.ui.LectureViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.SubjectTagChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LectureDetailSheet(
    lectureWithTags: LectureWithTags,
    viewModel: LectureViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lecture = lectureWithTags.lecture

    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val playingFilePath by viewModel.playerManager.currentFilePath.collectAsState()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerManager.durationMs.collectAsState()
    val currentSpeed by viewModel.playerManager.currentSpeed.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    val isPlayingThis = isPlaying && playingFilePath == lecture.audioFilePath

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Текст, 1: Статистика
    var showEditTagsDialog by remember { mutableStateOf(false) }
    var inSheetSearchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var textSizeSp by remember { mutableIntStateOf(14) } // Font size modifier

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
    val dateStr = dateFormat.format(Date(lecture.dateTimestamp))

    // Analytics calculations
    val fullText = lecture.transcriptionText
    val words = remember(fullText) {
        fullText.split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    val wordCount = words.size
    val charCount = fullText.length
    val paragraphList = remember(fullText) {
        fullText.split("\n").filter { it.isNotBlank() }
    }
    val paragraphCount = paragraphList.size
    val readingTimeMin = maxOf(1, (wordCount / 130.0).toInt())
    val wordsPerMinute = if (lecture.durationSeconds > 0) {
        (wordCount.toDouble() / (lecture.durationSeconds / 60.0)).toInt()
    } else 0

    // Top Keywords extraction
    val topKeywords = remember(words) {
        words.map { it.lowercase().replace(Regex("[^a-zа-я0-9]"), "") }
            .filter { it.length > 3 && !isStopWord(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.playerManager.stop()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxHeight(0.94f)
            .testTag("lecture_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Sheet Header Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = lecture.status)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.copyToClipboard(fullText) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.exportPdf(lectureWithTags) }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.shareText(lectureWithTags) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        viewModel.playerManager.stop()
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title & Date
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateStr,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subject Tags & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (lectureWithTags.tags.isEmpty()) {
                        Text(
                            text = "Без предметов",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        lectureWithTags.tags.forEach { tag ->
                            SubjectTagChip(tag = tag)
                        }
                    }
                }

                TextButton(onClick = { showEditTagsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Tags",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Теги", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Player Bar
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = {
                                if (isPlayingThis) {
                                    viewModel.playerManager.pause()
                                } else {
                                    viewModel.playerManager.playAudio(lecture.audioFilePath)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlayingThis) "Пауза" else "Слушать запись",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        // Speed buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                val isSel = currentSpeed == speed
                                Surface(
                                    onClick = { viewModel.playerManager.setPlaybackSpeed(speed) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Seekbar
                    val activePosition = if (isPlayingThis) currentPositionMs.toFloat() else 0f
                    val maxDuration = if (isPlayingThis && durationMs > 0) durationMs.toFloat() else (lecture.durationSeconds * 1000).toFloat()

                    Slider(
                        value = activePosition.coerceIn(0f, maxDuration.coerceAtLeast(1f)),
                        onValueChange = {
                            if (isPlayingThis) viewModel.playerManager.seekTo(it.toInt())
                        },
                        valueRange = 0f..maxDuration.coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(activePosition.toInt()),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTimeMs(maxDuration.toInt()),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs inside Detail Sheet
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Конспект лекции", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = "Text", modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Статистика урока", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics", modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab 0: Text Content with Search & Font Size
            if (selectedTab == 0) {
                // Toolbar for Text Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSearchField = !showSearchField }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search text",
                                tint = if (showSearchField) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Text Size Controls
                        Text("Шрифт:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { if (textSizeSp > 11) textSizeSp -= 2 }, modifier = Modifier.size(28.dp)) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("${textSizeSp}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (textSizeSp < 22) textSizeSp += 2 }, modifier = Modifier.size(28.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Text(
                        text = "$wordCount слов • ~$readingTimeMin мин чтения",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // In-sheet Search Field
                AnimatedVisibility(visible = showSearchField) {
                    OutlinedTextField(
                        value = inSheetSearchQuery,
                        onValueChange = { inSheetSearchQuery = it },
                        placeholder = { Text("Поиск слова по тексту лекции...") },
                        singleLine = true,
                        trailingIcon = {
                            if (inSheetSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { inSheetSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (lecture.status == TranscriptionStatus.PENDING_INTERNET) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Аудиозапись ждет подключения к интернету",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Файл сохранен локально. Как только появится связь, Gemini API мгновенно переведет речь в текст.",
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.retryTranscription(lecture.id) },
                                    modifier = Modifier.testTag("force_transcribe_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Попробовать сейчас")
                                }
                            }
                        }
                    } else if (lecture.status == TranscriptionStatus.ERROR) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Ошибка при расшифровке",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = lecture.errorMessage ?: "Проверьте интернет-соединение или API ключ",
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.retryTranscription(lecture.id) }
                                ) {
                                    Text("Повторить расшифровку")
                                }
                            }
                        }
                    } else {
                        val filteredParagraphs = if (inSheetSearchQuery.isBlank()) {
                            paragraphList
                        } else {
                            paragraphList.filter { it.contains(inSheetSearchQuery, ignoreCase = true) }
                        }

                        if (filteredParagraphs.isEmpty()) {
                            Text(
                                text = "Фразы с '$inSheetSearchQuery' не найдены.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            filteredParagraphs.forEachIndexed { index, paragraph ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontSize = (textSizeSp - 2).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = paragraph.trim(),
                                        fontSize = textSizeSp.sp,
                                        lineHeight = (textSizeSp * 1.6).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Comprehensive Lecture Statistics Screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Детальная статистика речи и текста",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid cards for statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(
                            title = "Всего слов",
                            value = "$wordCount",
                            subtitle = "лексический объем",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "Символов",
                            value = "$charCount",
                            subtitle = "знаков с пробелами",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(
                            title = "Абзацев / Мыслей",
                            value = "$paragraphCount",
                            subtitle = "смысловых блоков",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "Время чтения",
                            value = "~$readingTimeMin мин",
                            subtitle = "средний темп",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(
                            title = "Длительность",
                            value = "${lecture.durationSeconds / 60}м ${lecture.durationSeconds % 60}с",
                            subtitle = "аудиозапись",
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "Темп диктора",
                            value = if (wordsPerMinute > 0) "$wordsPerMinute слов/мин" else "—",
                            subtitle = "скорость речи",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Terms section
                    if (topKeywords.isNotEmpty()) {
                        Text(
                            text = "Ключевые понятия лекции",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topKeywords.forEach { term ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "#$term",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // File Technical Details
                    Text(
                        text = "Информация о файле",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Путь к файлу: ${lecture.audioFilePath}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Идентификатор лекции: ID #${lecture.id}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Модель распознавания: Gemini 2.5 Flash", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Edit Tags Dialog
            if (showEditTagsDialog) {
                var selectedIds by remember {
                    mutableStateOf(lectureWithTags.tags.map { it.id }.toSet())
                }

                AlertDialog(
                    onDismissRequest = { showEditTagsDialog = false },
                    title = { Text("Привязать предметы/теги") },
                    text = {
                        Column {
                            allTags.forEach { tag ->
                                val isChecked = selectedIds.contains(tag.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectTagChip(
                                        tag = tag,
                                        isSelected = isChecked,
                                        onClick = {
                                            selectedIds = if (isChecked) {
                                                selectedIds - tag.id
                                            } else {
                                                selectedIds + tag.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.updateLectureTags(lecture.id, selectedIds.toList())
                            showEditTagsDialog = false
                        }) {
                            Text("Применить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditTagsDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

fun isStopWord(word: String): Boolean {
    val stopWords = setOf("и", "в", "не", "на", "я", "с", "что", "по", "а", "это", "как", "то", "но", "из", "у", "от", "за", "для", "о", "же", "все", "так", "его", "или", "бы", "очень", "уже", "вы", "при", "даже", "только", "был", "где", "там", "чем", "себя", "нас", "меня", "вам", "этом", "этого", "свое", "эти")
    return word in stopWords
}

fun formatTimeMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
