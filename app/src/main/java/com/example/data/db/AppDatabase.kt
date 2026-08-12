package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LectureEntity::class, TagEntity::class, LectureTagCrossRef::class, TelemetryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lectureDao(): LectureDao
    abstract fun tagDao(): TagDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lecture_notes_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Populate default subjects/tags
                        CoroutineScope(Dispatchers.IO).launch {
                            val tagDao = getDatabase(context).tagDao()
                            tagDao.insertTag(TagEntity(name = "Матанализ", colorHex = "#3F51B5"))
                            tagDao.insertTag(TagEntity(name = "История", colorHex = "#E91E63"))
                            tagDao.insertTag(TagEntity(name = "Физика", colorHex = "#009688"))
                            tagDao.insertTag(TagEntity(name = "Программирование", colorHex = "#4CAF50"))
                            tagDao.insertTag(TagEntity(name = "Философия", colorHex = "#FF9800"))
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
