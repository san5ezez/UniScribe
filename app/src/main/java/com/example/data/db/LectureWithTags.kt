package com.example.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class LectureWithTags(
    @Embedded val lecture: LectureEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = LectureTagCrossRef::class,
            parentColumn = "lectureId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
