package com.example.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "lecture_tag_cross_ref",
    primaryKeys = ["lectureId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class LectureTagCrossRef(
    val lectureId: Long,
    val tagId: Long
)
