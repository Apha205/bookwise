package com.bookwise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"], unique = true)]
)
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val locatorJson: String?,
    val progression: Float?,
    val chapterHref: String?,
    val chapterTitle: String?,
    val totalProgress: Double?,
    val updatedAtMillis: Long
)
