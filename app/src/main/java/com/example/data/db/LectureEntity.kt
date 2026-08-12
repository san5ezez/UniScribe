package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TranscriptionStatus {
    PENDING_INTERNET,  // Ожидает интернета для расшифровки
    TRANSCRIBING,      // Идет расшифровка...
    TRANSCRIBED,       // Расшифровано
    ERROR              // Ошибка расшифровки
}

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val audioFilePath: String,
    val status: TranscriptionStatus = TranscriptionStatus.PENDING_INTERNET,
    val errorMessage: String? = null,
    val transcriptionText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
