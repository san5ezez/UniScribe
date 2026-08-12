package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import com.example.ui.detail.LectureDetailSheet
import com.example.ui.settings.SettingsBottomSheet
import com.example.ui.tabs.AggregatedNotesTab
import com.example.ui.tabs.LecturesTab
import com.example.ui.tabs.RecordingTab
import com.example.ui.tabs.TagsTab

enum class MainTab(val title: String) {
    RECORD("Запись"),
    LECTURES("Занятия"),
    AGGREGATED("Общий конспект"),
    TAGS("Предметы")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LectureViewModel
) {
    var selectedTabItem by remember { mutableIntStateOf(0) }
    val tabs = MainTab.values()

    val selectedLecture by viewModel.selectedLecture.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isQueueActive by viewModel.isTranscribingQueue.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Умный конспект",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Автозапись и расшифровка лекций",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Queue progress indicator
                    if (isQueueActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Фон...", fontSize = 10.sp)
                        }
                    }

                    // Network status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = "Online Status",
                                tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "Онлайн" else "Офлайн",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                    // Settings Gear Button
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.setShowSettings(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabItem == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabItem = index },
                        icon = {
                            when (tab) {
                                MainTab.RECORD -> Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = tab.title
                                )
                                MainTab.LECTURES -> Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = tab.title
                                )
                                MainTab.AGGREGATED -> Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = tab.title
                                )
                                MainTab.TAGS -> Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tabs[selectedTabItem]) {
                MainTab.RECORD -> RecordingTab(viewModel = viewModel)
                MainTab.LECTURES -> LecturesTab(
                    viewModel = viewModel,
                    onLectureClick = { lecture ->
                        viewModel.selectLectureForDetail(lecture)
                    }
                )
                MainTab.AGGREGATED -> AggregatedNotesTab(viewModel = viewModel)
                MainTab.TAGS -> TagsTab(viewModel = viewModel)
            }
        }

        // Detail Bottom Sheet
        selectedLecture?.let { lecture ->
            LectureDetailSheet(
                lectureWithTags = lecture,
                viewModel = viewModel,
                onDismiss = { viewModel.selectLectureForDetail(null) }
            )
        }

        // Settings Bottom Sheet
        if (showSettings) {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowSettings(false) }
            )
        }
    }
}
