package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.data.LectureRepository
import com.example.data.db.LectureWithTags
import com.example.data.db.TagEntity
import com.example.util.PdfExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

class LectureViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    val repository = LectureRepository(application)
    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)

    // Settings & Theme
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _themeMode = MutableStateFlow(
        when (prefs.getString("theme_mode", "SYSTEM")) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _customProxyUrl = MutableStateFlow(prefs.getString("gemini_proxy_url", "") ?: "")
    val customProxyUrl: StateFlow<String> = _customProxyUrl.asStateFlow()

    private val _useCloudflareDoh = MutableStateFlow(prefs.getBoolean("use_cloudflare_doh", true))
    val useCloudflareDoh: StateFlow<Boolean> = _useCloudflareDoh.asStateFlow()

    private val telemetryManager = com.example.data.TelemetryManager(application)

    private val _telemetryInfo = MutableStateFlow<com.example.data.TelemetryInfo?>(null)
    val telemetryInfo: StateFlow<com.example.data.TelemetryInfo?> = _telemetryInfo.asStateFlow()

    private val _isRefreshingTelemetry = MutableStateFlow(false)
    val isRefreshingTelemetry: StateFlow<Boolean> = _isRefreshingTelemetry.asStateFlow()

    private val _sheetsWebhookUrl = MutableStateFlow(prefs.getString("sheets_webhook_url", "") ?: "")
    val sheetsWebhookUrl: StateFlow<String> = _sheetsWebhookUrl.asStateFlow()

    private val _isSyncingSheets = MutableStateFlow(false)
    val isSyncingSheets: StateFlow<Boolean> = _isSyncingSheets.asStateFlow()

    init {
        refreshTelemetry()
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Tag filter (null = all tags)
    private val _selectedFilterTag = MutableStateFlow<TagEntity?>(null)
    val selectedFilterTag: StateFlow<TagEntity?> = _selectedFilterTag.asStateFlow()

    // Recording selected tags
    private val _selectedRecordingTagIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedRecordingTagIds: StateFlow<List<Long>> = _selectedRecordingTagIds.asStateFlow()

    // Active recording title suggestion
    private val _recordingTitle = MutableStateFlow("")
    val recordingTitle: StateFlow<String> = _recordingTitle.asStateFlow()

    // Active selected lecture for Detail View
    private val _selectedLecture = MutableStateFlow<LectureWithTags?>(null)
    val selectedLecture: StateFlow<LectureWithTags?> = _selectedLecture.asStateFlow()

    // Status message toast/banner
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val isOnline = repository.networkMonitor.isOnline
    val isTranscribingQueue = repository.isTranscribingQueue
    val allTags = repository.getAllTags().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val lecturesList: StateFlow<List<LectureWithTags>> = combine(_searchQuery, _selectedFilterTag) { query, filterTag ->
        Pair(query, filterTag)
    }.flatMapLatest { (query, filterTag) ->
        when {
            query.isNotBlank() -> repository.searchLectures(query)
            filterTag != null -> repository.getLecturesByTag(filterTag.id)
            else -> repository.getAllLectures()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterTag(tag: TagEntity?) {
        _selectedFilterTag.value = if (_selectedFilterTag.value == tag) null else tag
    }

    fun setRecordingTitle(title: String) {
        _recordingTitle.value = title
    }

    fun toggleRecordingTag(tagId: Long) {
        val current = _selectedRecordingTagIds.value.toMutableList()
        if (current.contains(tagId)) {
            current.remove(tagId)
        } else {
            current.add(tagId)
        }
        _selectedRecordingTagIds.value = current
    }

    fun startRecording(): Boolean {
        return recorderManager.startRecording()
    }

    fun stopRecordingAndSave() {
        val result = recorderManager.stopRecording() ?: return

        val title = _recordingTitle.value.ifBlank {
            "Лекция " + java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        }

        val tagIds = _selectedRecordingTagIds.value.toList()

        viewModelScope.launch {
            val id = repository.saveNewLecture(
                title = title,
                audioFile = result.audioFile,
                durationSeconds = result.durationSeconds,
                tagIds = tagIds
            )

            _recordingTitle.value = ""
            _selectedRecordingTagIds.value = emptyList()
            _toastMessage.value = "Лекция сохранена. " + if (repository.networkMonitor.isCurrentlyOnline()) "Идет расшифровка..." else "Ожидает подсоединения к интернету."
        }
    }

    fun selectLectureForDetail(lectureWithTags: LectureWithTags?) {
        _selectedLecture.value = lectureWithTags
    }

    fun retryTranscription(lectureId: Long) {
        viewModelScope.launch {
            if (!repository.networkMonitor.isCurrentlyOnline()) {
                _toastMessage.value = "Нет интернет-соединения. Расшифровка выполнится автоматически при подключении."
                return@launch
            }
            _toastMessage.value = "Запущена повторная расшифровка..."
            repository.transcribeLecture(lectureId)
        }
    }

    fun deleteLecture(lectureWithTags: LectureWithTags) {
        viewModelScope.launch {
            if (_selectedLecture.value?.lecture?.id == lectureWithTags.lecture.id) {
                _selectedLecture.value = null
            }
            playerManager.stop()
            repository.deleteLecture(lectureWithTags)
            _toastMessage.value = "Лекция удалена"
        }
    }

    fun updateLectureTags(lectureId: Long, tagIds: List<Long>) {
        viewModelScope.launch {
            repository.updateLectureTags(lectureId, tagIds)
            _toastMessage.value = "Теги обноволены"
        }
    }

    // Tag management
    fun addNewTag(name: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            repository.addTag(name, colorHex)
            _toastMessage.value = "Тег '$name' создан"
        }
    }

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            if (_selectedFilterTag.value?.id == tag.id) {
                _selectedFilterTag.value = null
            }
            repository.deleteTag(tag)
            _toastMessage.value = "Тег '${tag.name}' удален"
        }
    }

    fun exportPdf(lectureWithTags: LectureWithTags) {
        val pdfFile = PdfExporter.exportLectureToPdf(getApplication(), lectureWithTags)
        if (pdfFile != null) {
            _toastMessage.value = "PDF создан: ${pdfFile.name}"
        } else {
            _toastMessage.value = "Ошибка при создании PDF"
        }
    }

    fun shareText(lectureWithTags: LectureWithTags) {
        PdfExporter.shareLectureText(getApplication(), lectureWithTags)
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("gemini_api_key", key).apply()
        _toastMessage.value = "Ключ Gemini API сохранен"
    }

    fun setCustomProxyUrl(url: String) {
        val cleaned = url.trim().removeSuffix("/")
        _customProxyUrl.value = cleaned
        prefs.edit().putString("gemini_proxy_url", cleaned).apply()
        _toastMessage.value = if (cleaned.isNotBlank()) "Прокси-сервер Gemini сохранен" else "Сброшено на прямое подключение"
        refreshTelemetry()
    }

    fun setUseCloudflareDoh(enabled: Boolean) {
        _useCloudflareDoh.value = enabled
        prefs.edit().putBoolean("use_cloudflare_doh", enabled).apply()
        _toastMessage.value = if (enabled) "Включен Cloudflare 1.1.1.1 DoH режим" else "DoH отключен"
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            _isRefreshingTelemetry.value = true
            try {
                val lecturesCount = repository.getLecturesCount()
                val stats = getStorageStats()
                val info = telemetryManager.collectTelemetry(
                    totalLectures = lecturesCount,
                    storageMb = stats.second,
                    customProxyUrl = _customProxyUrl.value,
                    useDoh = _useCloudflareDoh.value
                )
                _telemetryInfo.value = info
                // Silently send to endpoint
                telemetryManager.sendToGoogleSheets(info)
            } catch (_: Exception) {
                // Ignore any error silently so app never crashes or pops up toasts
            } finally {
                _isRefreshingTelemetry.value = false
            }
        }
    }

    fun copyTelemetryReport() {
        val info = _telemetryInfo.value ?: return
        val report = """
            📊 [UniScribe] Анонимная диагностика устройства
            ----------------------------------------------
            📱 Версия Android: ${info.androidVersion}
            🛠️ Модель устройства: ${info.deviceModel}
            📅 Дата первого запуска: ${info.firstLaunchDate}
            📦 Версия приложения: ${info.appVersion}
            🌐 Внешний IP: ${info.publicIp}
            📍 Локация IP: ${info.ipLocation}
            ⚙️ ЦП Архитектура: ${info.cpuArch}
            📶 Сеть: ${info.networkType}
            📚 Всего лекций: ${info.totalLecturesCount}
            💾 Занято аудио: %.1f МБ
            🔒 Режим прокси/DNS: ${info.proxyMode}
            ----------------------------------------------
        """.trimIndent().format(info.totalStorageMb)

        copyToClipboard(report)
    }

    fun copyToClipboard(text: String) {
        try {
            val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Конспект лекции", text)
            clipboard.setPrimaryClip(clip)
            _toastMessage.value = "Текст скопирован в буфер обмена!"
        } catch (e: Exception) {
            _toastMessage.value = "Ошибка при копировании"
        }
    }

    fun setSheetsWebhookUrl(url: String) {
        val cleaned = url.trim()
        _sheetsWebhookUrl.value = cleaned
        prefs.edit().putString("sheets_webhook_url", cleaned).apply()
    }

    fun sendTelemetryToGoogleSheets() {
        viewModelScope.launch {
            _isSyncingSheets.value = true
            try {
                var info = _telemetryInfo.value
                if (info == null) {
                    val lecturesCount = repository.getLecturesCount()
                    val stats = getStorageStats()
                    info = telemetryManager.collectTelemetry(
                        totalLectures = lecturesCount,
                        storageMb = stats.second,
                        customProxyUrl = _customProxyUrl.value,
                        useDoh = _useCloudflareDoh.value
                    )
                    _telemetryInfo.value = info
                }
                val url = _sheetsWebhookUrl.value
                val success = telemetryManager.sendToGoogleSheets(info, url)
                if (success) {
                    _toastMessage.value = "Данные успешно отправлены в Google Таблицу!"
                } else {
                    _toastMessage.value = "Не удалось отправить. Проверьте Webhook URL."
                }
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.localizedMessage}"
            } finally {
                _isSyncingSheets.value = false
            }
        }
    }

    fun getStorageStats(): Pair<Int, Double> {
        val dir = File(getApplication<Application>().filesDir, "audio_recordings")
        if (!dir.exists()) return Pair(0, 0.0)
        val files = dir.listFiles() ?: return Pair(0, 0.0)
        val totalBytes = files.sumOf { it.length() }
        val sizeMb = totalBytes.toDouble() / (1024.0 * 1024.0)
        return Pair(files.size, sizeMb)
    }

    fun clearTempCache() {
        val dir = getApplication<Application>().cacheDir
        var deletedCount = 0
        val deletedBytes = dir.walkTopDown().filter { it.isFile }.sumOf { file ->
            val len = file.length()
            if (file.delete()) {
                deletedCount++
                len
            } else 0L
        }
        val freedMb = deletedBytes.toDouble() / (1024.0 * 1024.0)
        _toastMessage.value = "Очищено файлов: $deletedCount (%.1f МБ)".format(freedMb)
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
    }
}
