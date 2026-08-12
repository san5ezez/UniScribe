package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {

    @Transaction
    @Query("SELECT * FROM lectures ORDER BY dateTimestamp DESC")
    fun getAllLecturesWithTags(): Flow<List<LectureWithTags>>

    @Transaction
    @Query("SELECT * FROM lectures WHERE id = :id")
    fun getLectureWithTagsById(id: Long): Flow<LectureWithTags?>

    @Transaction
    @Query("SELECT * FROM lectures WHERE id = :id")
    suspend fun getLectureWithTagsByIdSync(id: Long): LectureWithTags?

    @Transaction
    @Query("SELECT * FROM lectures WHERE transcriptionText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY dateTimestamp DESC")
    fun searchLectures(query: String): Flow<List<LectureWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT l.* FROM lectures l 
        INNER JOIN lecture_tag_cross_ref ref ON l.id = ref.lectureId 
        WHERE ref.tagId = :tagId 
        ORDER BY l.dateTimestamp DESC
    """)
    fun getLecturesByTag(tagId: Long): Flow<List<LectureWithTags>>

    @Query("SELECT * FROM lectures WHERE status = 'PENDING_INTERNET' OR status = 'ERROR' ORDER BY dateTimestamp ASC")
    suspend fun getPendingLectures(): List<LectureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectureTagCrossRef(crossRef: LectureTagCrossRef)

    @Query("DELETE FROM lecture_tag_cross_ref WHERE lectureId = :lectureId")
    suspend fun deleteLectureTagCrossRefsForLecture(lectureId: Long)

    @Query("DELETE FROM lecture_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteLectureTagCrossRefsForTag(tagId: Long)

    @Transaction
    suspend fun setTagsForLecture(lectureId: Long, tagIds: List<Long>) {
        deleteLectureTagCrossRefsForLecture(lectureId)
        tagIds.forEach { tagId ->
            insertLectureTagCrossRef(LectureTagCrossRef(lectureId, tagId))
        }
    }
}
