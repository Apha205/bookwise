package com.bookwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["sha256"], unique = true),
        Index(value = ["fileName"])
    ]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val internalPath: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val title: String,
    val authorsJson: String,
    val coverPath: String?,
    val identifier: String?,
    val language: String?,
    val published: String?,
    val description: String?,
    val importedAtMillis: Long,
    val lastOpenedAtMillis: Long?,
    val isFavorite: Boolean = false,
    val citation: String? = null
)
