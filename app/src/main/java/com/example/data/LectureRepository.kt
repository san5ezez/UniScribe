package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.LectureEntity
import com.example.data.db.LectureWithTags
import com.example.data.db.TagEntity
import com.example.data.db.TranscriptionStatus
import com.example.data.remote.GeminiTranscriptionService
import com.example.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LectureRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val lectureDao = db.lectureDao()
    private val tagDao = db.tagDao()

    private val geminiService = GeminiTranscriptionService(context)
    val networkMonitor = NetworkMonitor(context)

    private val _isTranscribingQueue = MutableStateFlow(false)
    val isTranscribingQueue: StateFlow<Boolean> = _isTranscribingQueue.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "LectureRepository"
    }

    init {
        // Automatically listen to network recovery to process pending transcription queue
        networkMonitor.setOnNetworkAvailableListener {
            Log.d(TAG, "Network restored! Triggering background transcription queue...")
            processPendingQueue()
        }
    }

    fun getAllLectures(): Flow<List<LectureWithTags>> = lectureDao.getAllLecturesWithTags()

    fun getLectureById(id: Long): Flow<LectureWithTags?> = lectureDao.getLectureWithTagsById(id)

    fun searchLectures(query: String): Flow<List<LectureWithTags>> = lectureDao.searchLectures(query)

    fun getLecturesByTag(tagId: Long): Flow<List<LectureWithTags>> = lectureDao.getLecturesByTag(tagId)

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun saveNewLecture(
        title: String,
        audioFile: File,
        durationSeconds: Int,
        tagIds: List<Long>
    ): Long = withContext(Dispatchers.IO) {
        val newLecture = LectureEntity(
            title = title.ifBlank { "Лекция от " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()) },
            durationSeconds = durationSeconds,
            audioFilePath = audioFile.absolutePath,
            status = TranscriptionStatus.PENDING_INTERNET
        )

        val lectureId = lectureDao.insertLecture(newLecture)
        lectureDao.setTagsForLecture(lectureId, tagIds)

        // Try immediate transcription if internet is connected
        if (networkMonitor.isCurrentlyOnline()) {
            transcribeLecture(lectureId)
        } else {
            Log.d(TAG, "Offline mode: Lecture $lectureId saved for background transcription when online.")
        }

        lectureId
    }

    suspend fun transcribeLecture(lectureId: Long) = withContext(Dispatchers.IO) {
        val lectureWithTags = lectureDao.getLectureWithTagsByIdSync(lectureId) ?: return@withContext
        val lecture = lectureWithTags.lecture

        // Mark status as TRANSCRIBING
        lectureDao.updateLecture(
            lecture.copy(
                status = TranscriptionStatus.TRANSCRIBING,
                errorMessage = null
            )
        )

        val audioFile = File(lecture.audioFilePath)
        if (!audioFile.exists()) {
            lectureDao.updateLecture(
                lecture.copy(
                    status = TranscriptionStatus.ERROR,
                    errorMessage = "Аудиофайл не найден на устройстве"
                )
            )
            return@withContext
        }

        val result = geminiService.transcribeAudioFile(audioFile)

        result.fold(
            onSuccess = { transcribedText ->
                lectureDao.updateLecture(
                    lecture.copy(
                        status = TranscriptionStatus.TRANSCRIBED,
                        transcriptionText = transcribedText,
                        errorMessage = null
                    )
                )
                Log.d(TAG, "Successfully transcribed lecture #$lectureId")
            },
            onFailure = { error ->
                lectureDao.updateLecture(
                    lecture.copy(
                        status = if (networkMonitor.isCurrentlyOnline()) TranscriptionStatus.ERROR else TranscriptionStatus.PENDING_INTERNET,
                        errorMessage = error.localizedMessage ?: "Ошибка распознавания речи"
                    )
                )
                Log.e(TAG, "Failed transcription for lecture #$lectureId", error)
            }
        )
    }

    fun processPendingQueue() {
        scope.launch {
            if (_isTranscribingQueue.value) return@launch
            if (!networkMonitor.isCurrentlyOnline()) return@launch

            val pendingList = lectureDao.getPendingLectures()
            if (pendingList.isEmpty()) return@launch

            _isTranscribingQueue.value = true
            Log.d(TAG, "Found ${pendingList.size} pending lectures to transcribe in queue.")

            for (pending in pendingList) {
                if (!networkMonitor.isCurrentlyOnline()) break
                transcribeLecture(pending.id)
            }

            _isTranscribingQueue.value = false
        }
    }

    suspend fun updateLectureTags(lectureId: Long, tagIds: List<Long>) = withContext(Dispatchers.IO) {
        lectureDao.setTagsForLecture(lectureId, tagIds)
    }

    suspend fun updateLectureTitle(lectureId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val lectureWithTags = lectureDao.getLectureWithTagsByIdSync(lectureId) ?: return@withContext
        lectureDao.updateLecture(lectureWithTags.lecture.copy(title = newTitle))
    }

    suspend fun deleteLecture(lectureWithTags: LectureWithTags) = withContext(Dispatchers.IO) {
        // Also delete physical audio file if present
        val audioFile = File(lectureWithTags.lecture.audioFilePath)
        if (audioFile.exists()) {
            audioFile.delete()
        }
        lectureDao.deleteLecture(lectureWithTags.lecture)
    }

    // Tag operations
    suspend fun addTag(name: String, colorHex: String) = withContext(Dispatchers.IO) {
        val tag = TagEntity(name = name.trim(), colorHex = colorHex)
        tagDao.insertTag(tag)
    }

    suspend fun updateTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.updateTag(tag)
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        lectureDao.deleteLectureTagCrossRefsForTag(tag.id)
        tagDao.deleteTag(tag)
    }
}
